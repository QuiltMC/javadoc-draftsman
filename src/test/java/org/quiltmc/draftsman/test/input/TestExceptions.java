package org.quiltmc.draftsman.test.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class TestExceptions {
    public String readFileUnchecked() throws IOException {
        try (InputStream stream = TestExceptions.class.getResourceAsStream("file.txt")) {
            if (stream == null) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    public String readFile() {
        try {
            return readFileUnchecked();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the file", e);
        }
    }
}
