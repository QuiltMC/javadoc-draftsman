package org.quiltmc.draftsman;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class Util implements Opcodes {
    public static void pushTypeDefaultToStack(String descriptor, MethodVisitor visitor) {
        pushTypeDefaultToStack(Type.getType(descriptor), visitor);
    }

    public static void pushTypeDefaultToStack(Type type, MethodVisitor visitor) {
        switch (type.getSort()) {
            case Type.BYTE, Type.CHAR, Type.INT, Type.SHORT, Type.BOOLEAN -> visitor.visitInsn(ICONST_0);
            case Type.DOUBLE -> visitor.visitInsn(DCONST_0);
            case Type.FLOAT -> visitor.visitInsn(FCONST_0);
            case Type.LONG -> visitor.visitInsn(LCONST_0);
            default -> visitor.visitInsn(ACONST_NULL);
        }
    }

    public static void makeSimplestIPush(int val, MethodVisitor visitor) {
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

    public static int trySimplifyToIConst(int val) {
        // Try to use any of the ICONST_* instructions
        return switch (val) {
            case -1 -> ICONST_M1;
            case 0 -> ICONST_0;
            case 1 -> ICONST_1;
            case 2 -> ICONST_2;
            case 3 -> ICONST_3;
            case 4 -> ICONST_4;
            case 5 -> ICONST_5;
            default -> -1;
        };
    }

    public static boolean isConstantOpcode(int opcode) {
        return opcode >= ICONST_M1 && opcode <= LDC;
    }

    public static Object getConstantInsnValue(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
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
            if (insn instanceof LdcInsnNode ldcInsn) {
                return ldcInsn.cst;
            } else if (insn instanceof IntInsnNode intInsn) {
                return intInsn.operand;
            } else {
                // Should never happen
                throw new IllegalArgumentException("Unknown constant instruction: " + insn);
            }
        }
    }

    public static boolean isSameInsn(AbstractInsnNode a, AbstractInsnNode b) {
        if (a.getOpcode() != b.getOpcode()) {
            return false;
        }

        int opcode = a.getOpcode();
        if ((opcode >= BIPUSH && opcode <= SIPUSH) || opcode == NEWARRAY) {
            return ((IntInsnNode) a).operand == ((IntInsnNode) b).operand;
        } else if (opcode == LDC) {
            return ((LdcInsnNode) a).cst.equals(((LdcInsnNode) b).cst);
        } else if ((opcode >= ILOAD && opcode <= ALOAD) || (opcode >= ISTORE && opcode <= ASTORE) || opcode == RET) {
            return ((VarInsnNode) a).var == ((VarInsnNode) b).var;
        } else if (opcode == IINC) {
            IincInsnNode ai = (IincInsnNode) a;
            IincInsnNode bi = (IincInsnNode) b;
            return ai.var == bi.var && ai.incr == bi.incr;
        } else if ((opcode >= IFEQ && opcode <= JSR) || (opcode >= IFNULL && opcode <= IFNONNULL)) {
            return ((JumpInsnNode) a).label == ((JumpInsnNode) b).label;
        } else if (opcode == TABLESWITCH) {
            TableSwitchInsnNode ai = (TableSwitchInsnNode) a;
            TableSwitchInsnNode bi = (TableSwitchInsnNode) b;
            return ai.min == bi.min && ai.max == bi.max && ai.dflt == bi.dflt && ai.labels == bi.labels;
        } else if (opcode == LOOKUPSWITCH) {
            LookupSwitchInsnNode ai = (LookupSwitchInsnNode) a;
            LookupSwitchInsnNode bi = (LookupSwitchInsnNode) b;
            return ai.dflt == bi.dflt && ai.keys == bi.keys && ai.labels == bi.labels;
        } else if (opcode >= GETSTATIC && opcode <= PUTSTATIC) {
            FieldInsnNode ai = (FieldInsnNode) a;
            FieldInsnNode bi = (FieldInsnNode) b;
            return ai.owner.equals(bi.owner) && ai.name.equals(bi.name) && ai.desc.equals(bi.desc);
        } else if (opcode >= INVOKEVIRTUAL && opcode <= INVOKEINTERFACE) {
            MethodInsnNode ai = (MethodInsnNode) a;
            MethodInsnNode bi = (MethodInsnNode) b;
            return ai.owner.equals(bi.owner) && ai.name.equals(bi.name) && ai.desc.equals(bi.desc) && ai.itf == bi.itf;
        } else if (opcode == INVOKEDYNAMIC) {
            InvokeDynamicInsnNode ai = (InvokeDynamicInsnNode) a;
            InvokeDynamicInsnNode bi = (InvokeDynamicInsnNode) b;
            return ai.bsm == bi.bsm && ai.bsmArgs == bi.bsmArgs && ai.name.equals(bi.name) && ai.desc.equals(bi.desc);
        } else if (opcode == NEW || opcode == ANEWARRAY || opcode == CHECKCAST || opcode == INSTANCEOF) {
            TypeInsnNode ai = (TypeInsnNode) a;
            TypeInsnNode bi = (TypeInsnNode) b;
            return ai.desc.equals(bi.desc);
        } else if (opcode == MULTIANEWARRAY) {
            MultiANewArrayInsnNode ai = (MultiANewArrayInsnNode) a;
            MultiANewArrayInsnNode bi = (MultiANewArrayInsnNode) b;
            return ai.desc.equals(bi.desc) && ai.dims == bi.dims;
        } else {
            return true;
        }
    }
}
