package org.quiltmc.draftsman.test;

import org.quiltmc.draftsman.Draftsman;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DraftsmanJarTest {
    public static void main(String[] args) throws IOException {
        Path jar = Path.of(args[0]);
        Path outputPath = Path.of(args[1]);
        Path tmpDir = Files.createTempDirectory("draftsman-jar-contents");

        long start = System.currentTimeMillis();
        List<Path> classFiles = new ArrayList<>();
        JarFile jarFile = new JarFile(jar.toFile());
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                Path tmpFile = tmpDir.resolve(entry.getName());
                if (!Files.exists(tmpFile.getParent())) {
                    Files.createDirectories(tmpFile.getParent());
                }

                Files.copy(jarFile.getInputStream(entry), tmpFile);
                classFiles.add(tmpFile);
            }
        }

        System.out.println("Found " + classFiles.size() + " class files");
        Path outputFile = outputPath.resolve(jar.getFileName());
        if (!Files.exists(outputFile.getParent())) {
            Files.createDirectories(outputFile.getParent());
        }

        try (OutputStream outputStream = Files.newOutputStream(outputFile); ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            Map<Path, byte[]> transformedClasses = Draftsman.transformClasses(classFiles);
            for (Map.Entry<Path, byte[]> entry : transformedClasses.entrySet()) {
                Path relative = tmpDir.relativize(entry.getKey());
                ZipEntry zipEntry = new ZipEntry(relative.toString());
                zipOutputStream.putNextEntry(zipEntry);

                zipOutputStream.write(entry.getValue());
            }
        }
        long finish = System.currentTimeMillis();

        System.out.println("Transformed " + classFiles.size() + " class files in " + (finish - start) + "ms");
    }
}
