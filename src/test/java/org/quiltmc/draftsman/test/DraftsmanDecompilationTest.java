package org.quiltmc.draftsman.test;

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.quiltmc.draftsman.Draftsman;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DraftsmanDecompilationTest {
    public static void main(String[] args) throws URISyntaxException, IOException {
        Path basePath = Path.of(DraftsmanTest.class.getResource("DraftsmanTest.class").toURI()).getParent();
        Path outputPath = Path.of(args[0]);

        int fernflowerArgsStart = -1;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--")) {
                fernflowerArgsStart = i;
            }
        }

        Path inputPath;
        if (fernflowerArgsStart == -1 && args.length > 1) {
            inputPath = Path.of(args[1]);
        } else if (fernflowerArgsStart > 1) {
            inputPath = Path.of(args[1]);
        } else {
            inputPath = basePath.resolve("input");
        }

        long start = System.currentTimeMillis();
        List<Path> inputFiles = Files.walk(inputPath).filter(p -> !Files.isDirectory(p) && p.toString().endsWith(".class")).collect(java.util.stream.Collectors.toList());
        Map<Path, byte[]> transformedClasses = Draftsman.transformClasses(inputFiles, false, inputPath::relativize);

        Path tmpDir = Files.createTempDirectory("draftsman-decompilation-test");
        DraftsmanTest.writeClasses(tmpDir, transformedClasses);
        long transformEnd = System.currentTimeMillis();
        System.out.println("Class transformation took " + (transformEnd - start) + "ms");

        List<String> fernflowerArgs = new ArrayList<>();
        if (fernflowerArgsStart != -1) {
            fernflowerArgs.addAll(Arrays.asList(args).subList(fernflowerArgsStart + 1, args.length));
        }
        fernflowerArgs.add(tmpDir.toAbsolutePath().toString());
        fernflowerArgs.add(outputPath.toAbsolutePath().toString());

        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }

        ConsoleDecompiler.main(fernflowerArgs.toArray(new String[0]));
        long end = System.currentTimeMillis();
        System.out.println("Decompilation took " + (end - transformEnd) + "ms");
        System.out.println("Test took " + (end - start) + "ms");
    }
}
