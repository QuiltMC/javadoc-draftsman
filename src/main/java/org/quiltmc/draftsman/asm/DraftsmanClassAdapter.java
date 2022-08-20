package org.quiltmc.draftsman.asm;

import org.jetbrains.annotations.Contract;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.quiltmc.draftsman.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DraftsmanClassAdapter implements Opcodes {
    private static final VarInsnNode ALOAD_0 = new VarInsnNode(ALOAD, 0);

    public static void adapt(ClassReader reader, ClassVisitor visitor) {
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        adapt(classNode, visitor);
    }

    public static void adapt(ClassNode classNode, ClassVisitor visitor) {
        ClassNode newClassNode = adapt(classNode);
        newClassNode.accept(visitor);
    }

    @Contract("null -> null; !null -> new")
    public static ClassNode adapt(ClassNode classNode) {
        if (classNode == null) {
            return null;
        }

        ClassNode newClassNode = new ClassNode();

        // Copy data from the class
        newClassNode.version = classNode.version;
        newClassNode.access = classNode.access;
        newClassNode.name = classNode.name;
        newClassNode.signature = classNode.signature;
        newClassNode.superName = classNode.superName;
        newClassNode.interfaces = classNode.interfaces;
        newClassNode.sourceFile = classNode.sourceFile;
        newClassNode.sourceDebug = classNode.sourceDebug;
        newClassNode.module = classNode.module;
        newClassNode.outerClass = classNode.outerClass;
        newClassNode.outerMethod = classNode.outerMethod;
        newClassNode.outerMethodDesc = classNode.outerMethodDesc;
        newClassNode.visibleAnnotations = classNode.visibleAnnotations;
        newClassNode.invisibleAnnotations = classNode.invisibleAnnotations;
        newClassNode.visibleTypeAnnotations = classNode.visibleTypeAnnotations;
        newClassNode.invisibleTypeAnnotations = classNode.invisibleTypeAnnotations;
        newClassNode.attrs = classNode.attrs;
        newClassNode.innerClasses = classNode.innerClasses;
        newClassNode.nestHostClass = classNode.nestHostClass;
        newClassNode.nestMembers = classNode.nestMembers;
        newClassNode.permittedSubclasses = classNode.permittedSubclasses;
        newClassNode.recordComponents = classNode.recordComponents;
        newClassNode.fields = classNode.fields;

        Map<FieldNode, Object> staticFieldDefaultValues = getStaticFieldDefaultValues(classNode);
        Map<FieldNode, Object> fieldDefaultValues = getFieldDefaultValues(classNode);
        for (MethodNode method : classNode.methods) {
            switch (method.name) {
                case "<clinit>" -> newClassNode.methods.add(eraseClInit(method, classNode, staticFieldDefaultValues));
                case "<init>" -> newClassNode.methods.add(eraseInit(method, classNode, fieldDefaultValues));
                default -> newClassNode.methods.add(eraseMethod(method));
            }
        }

        return newClassNode;
    }

    private static <T extends AbstractInsnNode> T findInsnByOpcode(MethodNode method, int opcode) {
        for (AbstractInsnNode insn : method.instructions) {
            if (insn.getOpcode() == opcode) {
                //noinspection unchecked
                return (T) insn;
            }
        }
        return null;
    }

    private static List<Integer> getInsnsByOpcodeIndices(InsnList insns, int opcode) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < insns.size(); i++) {
            if (insns.get(i).getOpcode() == opcode) {
                indices.add(i);
            }
        }
        return indices;
    }

    private static List<Integer> getInsnsByOpcodeIndices(List<AbstractInsnNode> insns, int opcode) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < insns.size(); i++) {
            if (insns.get(i).getOpcode() == opcode) {
                indices.add(i);
            }
        }
        return indices;
    }

    private static void returnEndMethod(MethodVisitor m) {
        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
    }

    private static void throwEndMethod(MethodVisitor m) {
        m.visitTypeInsn(NEW, "java/lang/AbstractMethodError");
        m.visitInsn(DUP);
        m.visitMethodInsn(INVOKESPECIAL, "java/lang/AbstractMethodError", "<init>", "()V", false);
        m.visitInsn(ATHROW);
        m.visitMaxs(0, 0);
        m.visitEnd();
    }

    private static boolean isCanonicalConstructor(MethodNode init, ClassNode classNode) {
        if ((classNode.access & ACC_RECORD) == 0) {
            return false;
        }

        String argsDescriptor = classNode.recordComponents.stream().map(c -> c.descriptor).reduce("", (a, b) -> a + b);
        Type type = Type.getMethodType(String.format("(%s)V", argsDescriptor));
        return Type.getMethodType(init.desc).equals(type);
    }

    private static void addFieldValueToStack(FieldNode field, MethodVisitor visitor, Object value) {
        if (value == null) {
            Util.pushTypeDefaultToStack(field.desc, visitor);
            return;
        }

        if (value instanceof Integer val) {
            Util.makeSimplestIPush(val, visitor);
            return;
        }

        visitor.visitLdcInsn(value);
    }

    private static <V> V getForField(FieldNode field, Map<FieldNode, V> map) {
        return map.keySet().stream().filter(f -> matches(f, field)).findFirst().map(map::get).orElse(null);
    }

    private static boolean matches(FieldNode a, FieldNode b) {
        return a.name.equals(b.name) && a.desc.equals(b.desc);
    }

    private static Map<FieldNode, Object> getStaticFieldDefaultValues(ClassNode classNode) {
        MethodNode init = classNode.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElse(null);
        if (init == null) {
            return new HashMap<>();
        }

        List<Integer> putStatics = getInsnsByOpcodeIndices(init.instructions, PUTSTATIC);

        Map<FieldNode, Object> values = new HashMap<>();
        for (int putStaticIndex : putStatics) {
            if (putStaticIndex <= 0) {
                continue;
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
            FieldInsnNode putStatic = (FieldInsnNode) init.instructions.get(putStaticIndex);
            AbstractInsnNode prevInsn = init.instructions.get(putStaticIndex - 1);
            if (Util.isConstantOpcode(prevInsn.getOpcode())) {
                FieldNode field = new FieldNode(0, putStatic.name, putStatic.desc, null, null);
                values.put(field, Util.getConstantInsnValue(prevInsn));
            }
        }

        return values;
    }

    private static Map<FieldNode, Object> getFieldDefaultValues(ClassNode classNode) {
        List<MethodNode> inits = new ArrayList<>();
        for (MethodNode method : classNode.methods) {
            if (!method.name.equals("<init>")) {
                continue;
            }
            MethodInsnNode invokeSpecial = findInsnByOpcode(method, INVOKESPECIAL);
            // Exclude initializers with this() call
            if (invokeSpecial != null && !invokeSpecial.owner.equals(classNode.name)) {
                inits.add(method);
            }
        }

        // Find common insns among all initializers
        int maxCommonInsns = inits.stream().mapToInt(m -> m.instructions.size()).min().orElse(0);
        List<AbstractInsnNode> commonInsns = new ArrayList<>();
        for (int i = 0; i < maxCommonInsns; i++) {
            AbstractInsnNode insn = null;
            for (MethodNode method : inits) {
                AbstractInsnNode insn2 = method.instructions.get(i);
                if (insn == null) {
                    insn = insn2;
                } else if (!Util.isSameInsn(insn, insn2)) {
                    insn = null;
                    break;
                }
            }

            if (insn != null) {
                commonInsns.add(insn);
            }
        }

        List<Integer> putFields = getInsnsByOpcodeIndices(commonInsns, PUTFIELD);

        Map<FieldNode, Object> values = new HashMap<>();
        for (int putFieldIndex : putFields) {
            if (putFieldIndex <= 1) {
                continue;
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
            FieldInsnNode putField = (FieldInsnNode) commonInsns.get(putFieldIndex);
            AbstractInsnNode prevInsn = commonInsns.get(putFieldIndex - 1);
            AbstractInsnNode prevPrevInsn = commonInsns.get(putFieldIndex - 2);
            if (prevPrevInsn.getOpcode() == ALOAD && ((VarInsnNode) prevPrevInsn).var == 0) {
                if (Util.isConstantOpcode(prevInsn.getOpcode())) {
                    FieldNode field = new FieldNode(0, putField.name, putField.desc, null, null);
                    values.put(field, Util.getConstantInsnValue(prevInsn));
                }
            }
        }

        return values;
    }

    private static MethodNode eraseClInit(MethodNode original, ClassNode classNode, Map<FieldNode, Object> fieldDefaultValues) {
        MethodNode m = new MethodNode(original.access, original.name, original.desc, original.signature, original.exceptions.toArray(new String[0]));
        copyMethodData(original, m);
        m.visitCode();

        // Get an instance initializer
        MethodNode init = classNode.methods.stream()
                .filter(m2 -> m2.name.equals("<init>"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No instance initializer found"));
        Type[] initParams = Type.getMethodType(init.desc).getArgumentTypes();

        // Add enum field initializations
        int enumIndex = 0;
        for (FieldNode field : classNode.fields) {
            if ((field.access & ACC_ENUM) == 0) {
                continue;
            }

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
            m.visitTypeInsn(NEW, classNode.name);
            m.visitInsn(DUP);
            m.visitLdcInsn(field.name);
            Util.makeSimplestIPush(enumIndex++, m);

            for (int i = 2; i < initParams.length; i++) {
                Util.pushTypeDefaultToStack(initParams[i], m);
            }

            m.visitMethodInsn(INVOKESPECIAL, classNode.name, "<init>", init.desc, false);
            m.visitFieldInsn(PUTSTATIC, classNode.name, field.name, field.desc);
        }

        for (FieldNode field : classNode.fields) {
            if ((field.access & ACC_STATIC) == 0 || (field.access & ACC_ENUM) != 0 || field.value != null) {
                continue;
            }

            Object defaultValue = getForField(field, fieldDefaultValues);
            addFieldValueToStack(field, m, defaultValue != null ? defaultValue : field.value);

            m.visitFieldInsn(PUTSTATIC, classNode.name, field.name, field.desc);
        }

        returnEndMethod(m);
        return m;
    }

    private static MethodNode eraseInit(MethodNode original, ClassNode classNode, Map<FieldNode, Object> fieldDefaultValues) {
        MethodNode m = new MethodNode(original.access, original.name, original.desc, original.signature, original.exceptions.toArray(new String[0]));
        copyMethodData(original, m);
        m.visitCode();

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
        ALOAD_0.accept(m);

        // Get invokespecial from original method
        MethodInsnNode invokeSpecial = findInsnByOpcode(original, INVOKESPECIAL);

        if (invokeSpecial == null) {
            throw new RuntimeException("Could not find INVOKESPECIAL in constructor");
        }

        Type[] params = Type.getMethodType(invokeSpecial.desc).getArgumentTypes();
        for (Type param : params) {
            Util.pushTypeDefaultToStack(param, m);
        }

        invokeSpecial.accept(m);

        if (isCanonicalConstructor(original, classNode)) {
            // Create default canonical constructor
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
            for (RecordComponentNode component : classNode.recordComponents) {
                ALOAD_0.accept(m);

                Type type = Type.getType(component.descriptor);
                switch (type.getSort()) {
                    case Type.BYTE, Type.CHAR, Type.INT, Type.SHORT, Type.BOOLEAN -> m.visitVarInsn(ILOAD, i);
                    case Type.DOUBLE -> m.visitVarInsn(DLOAD, i++);
                    case Type.FLOAT -> m.visitVarInsn(FLOAD, i);
                    case Type.LONG -> m.visitVarInsn(LLOAD, i++);
                    default -> m.visitVarInsn(ALOAD, i);
                }
                i++;

                m.visitFieldInsn(PUTFIELD, classNode.name, component.name, component.descriptor);
            }

            returnEndMethod(m);
            return m;
        } else if (!invokeSpecial.owner.equals(classNode.name)) {
            // Add field initializations if the called constructor is a super constructor
            // (otherwise, we already did it in the called method)
            for (FieldNode field : classNode.fields) {
                if ((field.access & ACC_STATIC) != 0) {
                    continue;
                }

                ALOAD_0.accept(m);

                Object defaultValue = getForField(field, fieldDefaultValues);
                addFieldValueToStack(field, m, defaultValue != null ? defaultValue : field.value);

                m.visitFieldInsn(PUTFIELD, classNode.name, field.name, field.desc);
            }
        }

        throwEndMethod(m);
        return m;
    }

    private static MethodNode eraseMethod(MethodNode original) {
        if ((original.access & ACC_ABSTRACT) != 0) {
            return original; // Abstract methods don't need to be erased
        }

        MethodNode m = new MethodNode(original.access, original.name, original.desc, original.signature, original.exceptions.toArray(new String[0]));
        copyMethodData(original, m);

        m.visitCode();
        throwEndMethod(m);
        return m;
    }

    private static void copyMethodData(MethodNode original, MethodNode m) {
        m.parameters = original.parameters;
        m.visibleAnnotations = original.visibleAnnotations;
        m.invisibleAnnotations = original.invisibleAnnotations;
        m.visibleTypeAnnotations = original.visibleTypeAnnotations;
        m.invisibleTypeAnnotations = original.invisibleTypeAnnotations;
        m.attrs = original.attrs;
        m.annotationDefault = original.annotationDefault;
        m.visibleAnnotableParameterCount = original.visibleAnnotableParameterCount;
        m.visibleParameterAnnotations = original.visibleParameterAnnotations;
        m.invisibleAnnotableParameterCount = original.invisibleAnnotableParameterCount;
        m.invisibleParameterAnnotations = original.invisibleParameterAnnotations;

        copyParameterLvt(original, m);
    }

    private static void copyParameterLvt(MethodNode original, MethodNode m) {
        boolean isStatic = (original.access & ACC_STATIC) != 0;
        Type type = Type.getMethodType(original.desc);
        int maxIndex = (type.getArgumentsAndReturnSizes() >> 2) - (isStatic ? 1 : 0);

        List<LocalVariableNode> newLocals = new ArrayList<>();
        for (LocalVariableNode node : original.localVariables) {
            if (node.index > maxIndex) {
                break;
            }

            newLocals.add(node);
        }

        m.localVariables = newLocals;
    }
}
