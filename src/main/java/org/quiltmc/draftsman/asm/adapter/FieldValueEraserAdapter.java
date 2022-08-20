package org.quiltmc.draftsman.asm.adapter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.quiltmc.draftsman.Draftsman;
import org.quiltmc.draftsman.Util;
import org.quiltmc.draftsman.asm.Insn;
import org.quiltmc.draftsman.asm.visitor.InsnCollectorMethodVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FieldValueEraserAdapter extends ClassVisitor implements Opcodes {
    private final List<FieldData> noValueStaticFields = new ArrayList<>();
    private final List<FieldData> enumFields = new ArrayList<>();
    private final List<FieldData> instanceFields = new ArrayList<>();
    private final List<MethodData> instanceInitializers = new ArrayList<>();
    private InsnCollectorMethodVisitor clInitInsnCollector;
    private final Map<MethodData, List<Insn>> instanceInitializerInvokeSpecials = new HashMap<>();
    private final Map<MethodData, InsnCollectorMethodVisitor> instanceInitializerInsnCollectors = new HashMap<>();
    private final List<RecordComponent> recordComponents = new ArrayList<>();
    private String className;
    private boolean hasClinit = false;
    private boolean isRecord;
    private String recordCanonicalConstructorDescriptor = "";

    private Map<FieldData, Object> fieldDefaultValues;

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

            // Fix record component access flags (for a proper decompilation)
            if (isRecord && (access & ACC_PRIVATE) == 0) {
                access |= ACC_PRIVATE;
            }
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
            hasClinit = true;
            // Save instructions for later
            InsnCollectorMethodVisitor visitor = new InsnCollectorMethodVisitor(null);
            clInitInsnCollector = visitor;
            return visitor;
        } else if (name.equals("<init>")) {
            MethodData init = new MethodData(access, name, descriptor, signature, exceptions);
            instanceInitializers.add(init);

            // Save invokespecial instructions for later (we need the super/this constructor)
            InsnCollectorMethodVisitor visitor = new InsnCollectorMethodVisitor(null, INVOKESPECIAL);
            instanceInitializerInvokeSpecials.put(init, visitor.getInsns());
            // Save all instructions for later
            visitor = new InsnCollectorMethodVisitor(visitor);
            instanceInitializerInsnCollectors.put(init, visitor);

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

    private static boolean isConstantOpcode(int opcode) {
        return opcode >= ICONST_M1 && opcode <= LDC;
    }

    private static Object getConstantInsnValue(Insn insn) {
        int opcode = insn.opcode();
        if (!isConstantOpcode(opcode)) {
            throw new IllegalArgumentException("Not a constant instruction: " + insn);
        }

        if (opcode >= ICONST_M1 && opcode <= ICONST_5) {
            return opcode - ICONST_0;
        } else if (opcode >= LCONST_0 && opcode <= LCONST_1) {
            return opcode - LCONST_0;
        } else if (opcode >= FCONST_0 && opcode <= FCONST_2) {
            return opcode - FCONST_0;
        } else if (opcode >= DCONST_0 && opcode <= DCONST_1) {
            return opcode - DCONST_0;
        } else {
            return insn.getArg(0);
        }
    }

    private Map<FieldData, Object> getStaticFieldDefaultValues() {
        if (!hasClinit) {
            return Collections.emptyMap();
        }

        /*
         * Code:
         * static int staticField1 = 213;
         * static String staticField2 = "Hello world";
         * ====
         * Bytecode:
         * SIPUSH 213
         * PUTSTATIC com/example/TestClass.staticField1 : I
         * LDC "Hello world"
         * PUTSTATIC com/example/TestClass.staticField2 : Ljava/lang/String;
         */
        List<Insn> insns = clInitInsnCollector.getInsns();
        List<Integer> putStaticInsnIndexes = new ArrayList<>();
        for (int i = 0; i < insns.size(); i++) {
            Insn insn = insns.get(i);
            if (insn.opcode() == PUTSTATIC) {
                putStaticInsnIndexes.add(i);
            }
        }

        Map<FieldData, Object> values = new HashMap<>();
        for (int i : putStaticInsnIndexes) {
            if (i <= 0) {
                continue;
            }
            Insn insn = insns.get(i);
            Insn prevInsn = insns.get(i - 1);
            if (isConstantOpcode(prevInsn.opcode())) {
                String name = (String) insn.getArg(1);
                String descriptor = (String) insn.getArg(2);
                FieldData fieldData = new FieldData(0, name, descriptor, null, null);
                values.put(fieldData, getConstantInsnValue(prevInsn));
            }
        }

        return values;
    }

    private void computeFieldDefaultValues() {
        Map<MethodData, List<Insn>> initializerInsns = instanceInitializerInsnCollectors.entrySet().stream()
                .filter(e -> !hasThisConstructorCall(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getInsns()));

        // Get instructions common for each constructor
        int maxCommonInsns = initializerInsns.values().stream().mapToInt(List::size).min().orElse(0);
        List<Insn> commonInsns = new ArrayList<>();
        for (int i = 0; i < maxCommonInsns; i++) {
            Insn insn = null;
            for (List<Insn> insns : initializerInsns.values()) {
                if (insn == null) {
                    insn = insns.get(i);
                } else if (!insn.equals(insns.get(i))) {
                    break;
                }
            }
            commonInsns.add(insn);
        }

        /*
         * Code:
         * int field1 = -1;
         * String field2 = "Foo bar";
         * ====
         * Bytecode:
         * ALOAD 0
         * ICONST_M1
         * PUTFIELD com/example/TestClass.field1 : I
         * ALOAD 0
         * LDC "Foo bar"
         * PUTFIELD com/example/TestClass.field2 : Ljava/lang/String;
         */
        List<Integer> putFieldInsnIndexes = new ArrayList<>();
        for (int i = 0; i < commonInsns.size(); i++) {
            Insn insn = commonInsns.get(i);
            if (insn.opcode() == PUTFIELD) {
                putFieldInsnIndexes.add(i);
            }
        }

        Map<FieldData, Object> values = new HashMap<>();
        for (int i : putFieldInsnIndexes) {
            if (i <= 0) {
                continue;
            }
            Insn insn = commonInsns.get(i);
            Insn prevInsn = commonInsns.get(i - 1);
            Insn prevPrevInsn = commonInsns.get(i - 2);
            if (prevPrevInsn.opcode() == ALOAD && (Integer) prevPrevInsn.getArg(0) == 0) {
                if (isConstantOpcode(prevInsn.opcode())) {
                    String name = (String) insn.getArg(1);
                    String descrptor = (String) insn.getArg(2);
                    FieldData fieldData = new FieldData(0, name, descrptor, null, null);
                    values.put(fieldData, getConstantInsnValue(prevInsn));
                }
            }
        }
        fieldDefaultValues = values;
    }

    private Map<FieldData, Object> getFieldDefaultValues() {
        if (fieldDefaultValues == null) {
            computeFieldDefaultValues();
        }

        return fieldDefaultValues;
    }

    private void generateClInit() {
        if (!hasClinit) {
            return;
        }

        MethodVisitor visitor = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        visitor.visitCode();

        // Add enum field initializations
        int enumIndex = 0;
        for (FieldData field : enumFields) {
            /* Code:
             * NAME("value");
             * ====
             * Bytecode:
             * NEW com/example/TestEnum
             * DUP
             * LDC "NAME"
             * ICONST_0
             * LDC "value"
             * INVOKESPECIAL com/example/TestEnum.<init> (Ljava/lang/String;ILjava/lang/String;)V
             * PUTSTATIC com/example/TestEnum.NAME : Lcom/example/TestEnum;
             */
            visitor.visitTypeInsn(NEW, className);
            visitor.visitInsn(DUP);
            visitor.visitLdcInsn(field.name);
            Util.makeSimplestIPush(enumIndex++, visitor);
            MethodData initializer = instanceInitializers.get(0);
            Type type = Type.getMethodType(initializer.descriptor);
            Type[] initializerParams = type.getArgumentTypes();

            for (int i = 2; i < initializerParams.length; i++) {
                Util.pushTypeDefaultToStack(initializerParams[i], visitor);
            }

            visitor.visitMethodInsn(INVOKESPECIAL, className, "<init>", initializer.descriptor, false);
            visitor.visitFieldInsn(PUTSTATIC, className, field.name, field.descriptor);
        }

        Map<FieldData, Object> defaultValues = getStaticFieldDefaultValues();
        // Add static field initializations for the ones without an initial value
        for (FieldData field : noValueStaticFields) {
            /* Code:
             * static int staticField1 = 256;
             * ====
             * Bytecode:
             * SIPUSH 256
             * PUTSTATIC com/example/TestClass.staticField1 : I
             */
            // If the field has a default value, use it
            Optional<FieldData> defaultValueKey = defaultValues.keySet().stream().filter(f -> f.isSameField(field)).findAny();
            addFieldValueToStack(field, visitor, defaultValueKey.map(defaultValues::get).orElse(field.value));
            visitor.visitFieldInsn(PUTSTATIC, className, field.name, field.descriptor);
        }

        // We can't throw an exception here, so we just return.
        visitor.visitInsn(RETURN);
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
    }

    private void generateInits() {
        Map<FieldData, Object> defaultValues = getFieldDefaultValues();

        for (MethodData init : instanceInitializers) {
            MethodVisitor visitor = super.visitMethod(init.access, init.name, init.descriptor, init.signature, init.exceptions);
            visitor.visitCode();
            // Handle super/this constructor call
            /*
             * Code:
             * this(arg, arg2, i);
             * ====
             * Bytecode:
             * ALOAD 0
             * ALOAD 1
             * ALOAD 2
             * ILOAD 3
             * INVOKESPECIAL com/example/TestClass.<init> (Ljava/lang/String;Ljava/lang/Object;I)V
             */
            visitor.visitVarInsn(ALOAD, 0);

            Insn superInvokeSpecial = instanceInitializerInvokeSpecials.get(init).get(0); // Always the first one
            List<Object> superInvokeSpecialArgs = superInvokeSpecial.args();
            String descriptor = (String) superInvokeSpecialArgs.get(2);
            Type[] params = Type.getMethodType(descriptor).getArgumentTypes();
            for (Type param : params) {
                Util.pushTypeDefaultToStack(param, visitor);
            }

            visitor.visitMethodInsn(INVOKESPECIAL, (String) superInvokeSpecialArgs.get(0), (String) superInvokeSpecialArgs.get(1), (String) superInvokeSpecialArgs.get(2), (Boolean) superInvokeSpecialArgs.get(3));

            if (isRecord
                    // && isCanonicalConstructor
                    && init.descriptor.substring(1, init.descriptor.indexOf(')')).equals(recordCanonicalConstructorDescriptor)) {
                // Create default record canonical constructor
                /*
                 * Code: (implicit)
                 * this.field1 = field1;
                 * this.field2 = field2;
                 * ====
                 * Bytecode:
                 * ALOAD 0
                 * ALOAD 1
                 * PUTFIELD com/example/TestClass.field1 : Ljava/lang/Object;
                 * ALOAD 0
                 * ILOAD 2
                 * PUTFIELD com/example/TestClass.field2 : I
                 */
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

                visitor.visitInsn(RETURN);
                visitor.visitMaxs(0, 0);
                visitor.visitEnd();
                continue;
            } else if (superInvokeSpecialArgs.get(0) != className) {
                // Add field initializations if the called constructor is a super constructor
                // (otherwise, we already did it in the called method)
                for (FieldData field : instanceFields) {
                    visitor.visitVarInsn(ALOAD, 0);

                    // If the field has a default value, use it
                    Optional<FieldData> defaultValueKey = defaultValues.keySet().stream().filter(f -> f.isSameField(field)).findAny();
                    addFieldValueToStack(field, visitor, defaultValueKey.map(defaultValues::get).orElse(field.value));

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

    private static void addFieldValueToStack(FieldData field, MethodVisitor visitor, Object value) {
        if (value == null) {
            Util.pushTypeDefaultToStack(field.descriptor, visitor);

            return;
        }

        if (value instanceof Integer val) {
            Util.makeSimplestIPush(val, visitor);
            return;
        }

        visitor.visitLdcInsn(value);
    }

    /**
     * {@return whether the given method is a constructor and has a call to this()}
     */
    private boolean hasThisConstructorCall(MethodData method) {
        if (!instanceInitializers.contains(method)) {
            return false;
        }

        Insn invokeSpecial = instanceInitializerInvokeSpecials.get(method).get(0);
        String className = (String) invokeSpecial.getArg(0);
        return className.equals(this.className);
    }

    public record FieldData(int access, String name, String descriptor, String signature, Object value) {
        public boolean isSameField(FieldData other) {
            return other.name.equals(name) && other.descriptor.equals(descriptor);
        }
    }

    public record MethodData(int access, String name, String descriptor, String signature, String[] exceptions) {
    }

    public record RecordComponent(String name, String descriptor, String signature) {
    }
}
