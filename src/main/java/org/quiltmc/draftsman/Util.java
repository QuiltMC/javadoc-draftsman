package org.quiltmc.draftsman;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

public class Util {
    public static List<String> splitDescriptorParameters(String descriptor) {
        String desc = descriptor.substring(1, descriptor.indexOf(')'));
        List<String> params = new ArrayList<>();

        for (int i = 0; i < desc.length(); i++) {
            String param = getFirstDescriptor(desc.substring(i));
            params.add(param);
            i += param.length() - 1;
        }

        return params;
    }

    private static String getFirstDescriptor(String descriptor) {
        char c = descriptor.charAt(0);
        String d = switch (c) {
            case 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> String.valueOf(c);
            default -> null;
        };
        if (d != null) {
            return d;
        }

        if (c == 'L') {
            return descriptor.substring(0, descriptor.indexOf(";") + 1);
        }

        if (c == '[') {
            return c + getFirstDescriptor(descriptor.substring(1));
        }

        return null;
    }

    public static void addTypeDefaultToStack(String descriptor, MethodVisitor visitor) {
        switch (descriptor) {
            case "B", "C", "I", "S", "Z" -> visitor.visitInsn(Opcodes.ICONST_0);
            case "D" -> visitor.visitInsn(Opcodes.DCONST_0);
            case "F" -> visitor.visitInsn(Opcodes.FCONST_0);
            case "J" -> visitor.visitInsn(Opcodes.LCONST_0);
            default -> visitor.visitInsn(Opcodes.ACONST_NULL);
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
            visitor.visitIntInsn(Opcodes.BIPUSH, val);
            return;
        } else if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
            // If the value can be a short, we can use SIPUSH
            visitor.visitIntInsn(Opcodes.SIPUSH, val);
            return;
        }

        visitor.visitLdcInsn(val);
    }

    public static int trySimplifyToIConst(int val) {
        // Try to use any of the ICONST_* instructions
        return switch (val) {
            case 0 -> Opcodes.ICONST_0;
            case 1 -> Opcodes.ICONST_1;
            case 2 -> Opcodes.ICONST_2;
            case 3 -> Opcodes.ICONST_3;
            case 4 -> Opcodes.ICONST_4;
            case 5 -> Opcodes.ICONST_5;
            default -> -1;
        };
    }
}
