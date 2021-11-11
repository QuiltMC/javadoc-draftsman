package org.quiltmc.draftsman.test;

import org.quiltmc.draftsman.Draftsman;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        List<Path> inputFiles = Files.walk(inputPath).filter(p -> !Files.isDirectory(p) && p.toString().endsWith(".class")).collect(Collectors.toList());
        Map<Path, byte[]> transformedClasses = Draftsman.transformClasses(inputFiles, false, inputPath::relativize);

        Draftsman.writeClasses(outputPath, transformedClasses);
    }

}
