package org.quiltmc.draftsman;

import org.objectweb.asm.Opcodes;
import org.quiltmc.draftsman.asm.DraftsmanClassTransformer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Draftsman {
    public static final int ASM_VERSION = Opcodes.ASM9;

    public static void main(String[] args) {
        Path inputPath = Path.of(args[0]);
        Path outputPath = Path.of(args[1]);

        try {
            // TODO: Jar support
            List<Path> inputFiles = Files.walk(inputPath).filter(p -> !Files.isDirectory(p) && p.toString().endsWith(".class")).collect(Collectors.toList());

            Map<Path, byte[]> classFiles = transformClasses(inputFiles, false, inputPath::relativize);

            writeClasses(outputPath, classFiles);
        } catch (IOException e) {
            System.err.println("Failed to transform the class files");
            e.printStackTrace();
        }
    }

    public static Map<Path, byte[]> transformClasses(List<Path> paths, boolean trace, Function<Path, Path> pathProcessor) {
        Map<Path, byte[]> classFiles = new HashMap<>();

        for (Path path : paths) {
            try {
                byte[] classFile = Files.readAllBytes(path);
                DraftsmanClassTransformer transformer = new DraftsmanClassTransformer(classFile, trace);
                byte[] transformed = transformer.transform();
                classFiles.put(pathProcessor.apply(path), transformed);
            } catch (IOException e) {
                System.err.println("Failed to transform class file " + path);
                e.printStackTrace();
            }
        }

        return classFiles;
    }

    public static Map<Path, byte[]> transformClasses(List<Path> paths, boolean trace) {
        return transformClasses(paths, trace, Function.identity());
    }

    public static Map<Path, byte[]> transformClasses(List<Path> inputFiles) {
        return transformClasses(inputFiles, false);
    }

    public static void writeClasses(Path outputPath, Map<Path, byte[]> transformedClasses) throws IOException {
        for (Map.Entry<Path, byte[]> entry : transformedClasses.entrySet()) {
            Path outputFile = outputPath.resolve(entry.getKey());

            if (!Files.exists(outputFile.getParent())) {
                Files.createDirectories(outputFile.getParent());
            }

            Files.write(outputFile, entry.getValue());
        }
    }
}
