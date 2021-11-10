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
        Path inputPath = basePath.resolve("input");
        Path outputPath = Path.of(args[0]);

        List<Path> inputFiles = Files.walk(inputPath).filter(p -> !Files.isDirectory(p) && p.toString().endsWith(".class")).collect(java.util.stream.Collectors.toList());
        Map<Path, byte[]> transformedClasses = Draftsman.transformClasses(inputFiles, false, inputPath::relativize);

        Path tmpDir = Files.createTempDirectory("draftsman-decompilation-test");
        DraftsmanTest.writeClasses(tmpDir, transformedClasses);

        List<String> fernflowerArgs = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--")) {
                fernflowerArgs.addAll(Arrays.asList(args).subList(i + 1, args.length));
            }
        }
        fernflowerArgs.add(tmpDir.toAbsolutePath().toString());
        fernflowerArgs.add(outputPath.toAbsolutePath().toString());

        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }

        ConsoleDecompiler.main(fernflowerArgs.toArray(new String[0]));
    }
}
