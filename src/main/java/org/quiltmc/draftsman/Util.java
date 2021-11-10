package org.quiltmc.draftsman;

import java.util.ArrayList;
import java.util.List;

public class Util {
    public static List<String> splitDescriptorParameters(String descriptor) {
        String desc = descriptor.substring(1, descriptor.indexOf(')'));
        List<String> params = new ArrayList<>();

        for (int i = 0; i < desc.length(); i++) {
            char c = desc.charAt(i);
            StringBuilder param = new StringBuilder(String.valueOf(c));
            if (c == 'L') {
                for (; i < desc.length(); i++) {
                    char c2 = desc.charAt(i);
                    param.append(c2);
                    if (c2 == ';') {
                        break;
                    }
                }
            } else if (c == '[') {
                for (; i < desc.length(); i++) {
                    char c2 = desc.charAt(i);
                    param.append(c2);
                    if (c2 != '[') {
                        break;
                    }
                }
            }

             params.add(param.toString());
        }

        return params;
    }
}
