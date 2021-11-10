package org.quiltmc.draftsman;

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
}
