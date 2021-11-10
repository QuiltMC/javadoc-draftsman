package org.quiltmc.draftsman.asm.adapter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.quiltmc.draftsman.Draftsman;
import org.quiltmc.draftsman.Util;

import java.util.ArrayList;
import java.util.List;

public class FieldValueEraserAdapter extends ClassVisitor implements Opcodes {
    private final List<FieldData> noValueStaticFields = new ArrayList<>();
    private final List<FieldData> enumFields = new ArrayList<>();
    private final List<FieldData> instanceFields = new ArrayList<>();
    private final List<MethodData> instanceInitializers = new ArrayList<>();
    private String className;

    public FieldValueEraserAdapter(ClassVisitor classVisitor) {
        super(Draftsman.ASM_VERSION, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
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

        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.equals("<clinit>")) {
            return null; // Remove clinit
        } else if (name.equals("<init>")) {
            instanceInitializers.add(new MethodData(access, name, descriptor, signature, exceptions));
            return null; // Remove init
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
            makeSimplestIPush(enumIndex++, visitor);
            MethodData initializer = instanceInitializers.get(0);
            List<String> initializerParams = Util.splitDescriptorParameters(initializer.descriptor);

            for (int i = 2; i < initializerParams.size(); i++) {
                addTypeDefaultToStack(initializerParams.get(i), visitor);
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
            visitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            // Add field initializations
            for (FieldData field : instanceFields) {
                visitor.visitVarInsn(ALOAD, 0);

                addFieldValueToStack(field, visitor);

                visitor.visitFieldInsn(PUTFIELD, className, field.name, field.descriptor);
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
            addTypeDefaultToStack(field.descriptor, visitor);

            return;
        }

        if (value instanceof Integer val) {
            makeSimplestIPush(val, visitor);
            return;
        }

        visitor.visitLdcInsn(value);
    }

    private static void addTypeDefaultToStack(String descriptor, MethodVisitor visitor) {
        switch (descriptor) {
            case "B", "C", "I", "S", "Z" -> visitor.visitInsn(ICONST_0);
            case "D" -> visitor.visitInsn(DCONST_0);
            case "F" -> visitor.visitInsn(FCONST_0);
            case "J" -> visitor.visitInsn(LCONST_0);
            default -> visitor.visitInsn(ACONST_NULL);
        }
    }

    private static void makeSimplestIPush(int val, MethodVisitor visitor) {
        int iconst = trySimplifyToIConst(val);
        if (iconst != -1) {
            visitor.visitInsn(iconst);
            return;
        }

        if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
            // If the value can be a byte, we can use BIPUSH
            visitor.visitIntInsn(BIPUSH, val);
            return;
        } else if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
            // If the value can be a short, we can use SIPUSH
            visitor.visitIntInsn(SIPUSH, val);
            return;
        }

        visitor.visitLdcInsn(val);
    }

    private static int trySimplifyToIConst(int val) {
        // Try to use any of the ICONST_* instructions
        return switch (val) {
            case 0 -> ICONST_0;
            case 1 -> ICONST_1;
            case 2 -> ICONST_2;
            case 3 -> ICONST_3;
            case 4 -> ICONST_4;
            case 5 -> ICONST_5;
            default -> -1;
        };
    }

    public record FieldData(int access, String name, String descriptor, String signature, Object value) {
    }

    public record MethodData(int access, String name, String descriptor, String signature, String[] exceptions) {
    }
}
