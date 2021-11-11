package org.quiltmc.draftsman.asm.adapter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.quiltmc.draftsman.Draftsman;
import org.quiltmc.draftsman.Util;
import org.quiltmc.draftsman.asm.Insn;
import org.quiltmc.draftsman.asm.visitor.InsnCollectorMethodVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldValueEraserAdapter extends ClassVisitor implements Opcodes {
    private final List<FieldData> noValueStaticFields = new ArrayList<>();
    private final List<FieldData> enumFields = new ArrayList<>();
    private final List<FieldData> instanceFields = new ArrayList<>();
    private final List<MethodData> instanceInitializers = new ArrayList<>();
    private final Map<MethodData, List<Insn>> instanceInitializerInvokeSpecials = new HashMap<>();
    private List<RecordComponent> recordComponents = new ArrayList<>();
    private String className;
    private boolean isRecord;
    private String recordCanonicalConstructorDescriptor = "";

    public FieldValueEraserAdapter(ClassVisitor classVisitor) {
        super(Draftsman.ASM_VERSION, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        if ((access & ACC_RECORD) != 0) {
            isRecord = true;
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        FieldData fieldData = new FieldData(access, name, descriptor, signature, value);

        if ((access & ACC_STATIC) != 0) {
            if ((access & ACC_ENUM) != 0) {
                // Enum fields neeed to be initialized in <clinit>
                enumFields.add(fieldData);
            } else if (value == null) {
                // Static fields with a value don't need to be initialized in <clinit>
                noValueStaticFields.add(fieldData);
            }
        } else {
            // All instance fields need to be initialized in <init>
            instanceFields.add(fieldData);
        }

        // Fix record component access flags
        if (isRecord && (access & ACC_PRIVATE) == 0) {
            access |= ACC_PRIVATE;
        }

        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        this.recordComponents.add(new RecordComponent(name, descriptor, signature));
        recordCanonicalConstructorDescriptor += descriptor;

        return super.visitRecordComponent(name, descriptor, signature);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.equals("<clinit>")) {
            return null; // Remove clinit
        } else if (name.equals("<init>")) {
            MethodData init = new MethodData(access, name, descriptor, signature, exceptions);
            instanceInitializers.add(init);

            // Save invokespecial instructions for later (we need the super/this constructor)
            InsnCollectorMethodVisitor visitor = new InsnCollectorMethodVisitor(null, INVOKESPECIAL);
            instanceInitializerInvokeSpecials.put(init, visitor.getInsns());

            return visitor;
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        generateClInit();
        generateInits();

        super.visitEnd();
    }

    private void generateClInit() {
        MethodVisitor visitor = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        visitor.visitCode();

        // Add enum field initializations
        int enumIndex = 0;
        for (FieldData field : enumFields) {
            visitor.visitTypeInsn(NEW, className);
            visitor.visitInsn(DUP);
            visitor.visitLdcInsn(field.name);
            Util.makeSimplestIPush(enumIndex++, visitor);
            MethodData initializer = instanceInitializers.get(0);
            List<String> initializerParams = Util.splitDescriptorParameters(initializer.descriptor);

            for (int i = 2; i < initializerParams.size(); i++) {
                Util.addTypeDefaultToStack(initializerParams.get(i), visitor);
            }

            visitor.visitMethodInsn(INVOKESPECIAL, className, "<init>", initializer.descriptor, false);
            visitor.visitFieldInsn(PUTSTATIC, className, field.name, field.descriptor);
        }

        for (FieldData field : noValueStaticFields) {
            addFieldValueToStack(field, visitor);
            visitor.visitFieldInsn(PUTSTATIC, className, field.name, field.descriptor);
        }

        // We can't throw an exception here, so we just return.
        visitor.visitInsn(RETURN);
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
    }

    private void generateInits() {
        for (MethodData init : instanceInitializers) {
            MethodVisitor visitor = super.visitMethod(init.access, init.name, init.descriptor, init.signature, init.exceptions);
            visitor.visitCode();
            visitor.visitVarInsn(ALOAD, 0);

            // Handle super/this constructor call
            Insn superInvokeSpecial = instanceInitializerInvokeSpecials.get(init).get(0); // Always the first one
            List<Object> args = superInvokeSpecial.args();
            String descriptor = (String) args.get(2);
            List<String> params = Util.splitDescriptorParameters(descriptor);
            for (String param : params) {
                Util.addTypeDefaultToStack(param, visitor);
            }

            visitor.visitMethodInsn(INVOKESPECIAL, (String) args.get(0), (String) args.get(1), (String) args.get(2), (Boolean) args.get(3));

            if (isRecord
                    // && isCanonicalConstructor
                    && init.descriptor.substring(1, init.descriptor.indexOf(')')).equals(recordCanonicalConstructorDescriptor)) {
                // Add default record component initializations
                int i = 1;
                for (RecordComponent component : recordComponents) {
                    visitor.visitVarInsn(ALOAD, 0);

                    switch (component.descriptor) {
                        case "B", "C", "I", "S", "Z" -> visitor.visitVarInsn(ILOAD, i);
                        case "D" -> visitor.visitVarInsn(DLOAD, i++);
                        case "F" -> visitor.visitVarInsn(FLOAD, i);
                        case "J" -> visitor.visitVarInsn(LLOAD, i++);
                        default -> visitor.visitVarInsn(ALOAD, i);
                    }
                    i++;

                    visitor.visitFieldInsn(PUTFIELD, className, component.name, component.descriptor);
                }
            } else {
                // Add field initializations
                for (FieldData field : instanceFields) {
                    visitor.visitVarInsn(ALOAD, 0);

                    addFieldValueToStack(field, visitor);

                    visitor.visitFieldInsn(PUTFIELD, className, field.name, field.descriptor);
                }
            }

            visitor.visitTypeInsn(NEW, "java/lang/AbstractMethodError");
            visitor.visitInsn(DUP);
            visitor.visitMethodInsn(INVOKESPECIAL, "java/lang/AbstractMethodError", "<init>", "()V", false);
            visitor.visitInsn(ATHROW);
            visitor.visitMaxs(0, 0);
            visitor.visitEnd();
        }
    }

    private static void addFieldValueToStack(FieldData field, MethodVisitor visitor) {
        Object value = field.value;
        if (value == null) {
            Util.addTypeDefaultToStack(field.descriptor, visitor);

            return;
        }

        if (value instanceof Integer val) {
            Util.makeSimplestIPush(val, visitor);
            return;
        }

        visitor.visitLdcInsn(value);
    }

    public record FieldData(int access, String name, String descriptor, String signature, Object value) {
    }

    public record MethodData(int access, String name, String descriptor, String signature, String[] exceptions) {
    }

    public record RecordComponent(String name, String descriptor, String signature) {
    }
}
