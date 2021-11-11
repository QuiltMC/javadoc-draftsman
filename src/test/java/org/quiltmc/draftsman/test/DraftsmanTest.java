package org.quiltmc.draftsman.test;

import org.quiltmc.draftsman.Draftsman;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DraftsmanTest {
    public static void main(String[] args) throws IOException, URISyntaxException {
        Path basePath = Path.of(DraftsmanTest.class.getResource("DraftsmanTest.class").toURI()).getParent();
        Path outputPath = Path.of(args[0]);
        Path inputPath;
        if (args.length > 1) {
            inputPath = Path.of(args[1]);
        } else {
            inputPath = basePath.resolve("input");
        }

        List<Path> inputFiles = Files.walk(inputPath).filter(p -> !Files.isDirectory(p) && p.toString().endsWith(".class")).collect(java.util.stream.Collectors.toList());
        Map<Path, byte[]> transformedClasses = Draftsman.transformClasses(inputFiles, false, inputPath::relativize);

        writeClasses(outputPath, transformedClasses);
    }

    protected static void writeClasses(Path outputPath, Map<Path, byte[]> transformedClasses) throws IOException {
        for (Map.Entry<Path, byte[]> entry : transformedClasses.entrySet()) {
            Path outputFile = outputPath.resolve(entry.getKey());

            if (!Files.exists(outputFile.getParent())) {
                Files.createDirectories(outputFile.getParent());
            }

            Files.write(outputFile, entry.getValue());
        }
    }
}
