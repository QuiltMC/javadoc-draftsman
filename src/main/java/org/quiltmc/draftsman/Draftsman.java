package org.quiltmc.draftsman;

import org.objectweb.asm.Opcodes;
import org.quiltmc.draftsman.asm.DraftsmanClassTransformer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Draftsman {
    public static final int ASM_VERSION = Opcodes.ASM9;

    public static Map<Path, byte[]> transformClasses(List<Path> paths, boolean trace) {
        Map<Path, byte[]> classFiles = new HashMap<>();

        for (Path path : paths) {
            try {
                byte[] classFile = Files.readAllBytes(path);
                DraftsmanClassTransformer transformer = new DraftsmanClassTransformer(classFile, trace);
                byte[] transformed = transformer.transform();
                classFiles.put(path, transformed);
            } catch (IOException e) {
                System.err.println("Failed to transform class file " + path);
                e.printStackTrace();
            }
        }

        return classFiles;
    }

    public static Map<Path, byte[]> transformClasses(List<Path> inputFiles) {
        return transformClasses(inputFiles, false);
    }
}
