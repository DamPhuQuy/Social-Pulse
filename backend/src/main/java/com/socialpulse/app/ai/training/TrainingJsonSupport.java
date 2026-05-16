package com.socialpulse.app.ai.training;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.luben.zstd.ZstdInputStream;

final class TrainingJsonSupport {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TrainingJsonSupport() {
    }

    static String toPrettyJson(Object value) throws JsonProcessingException {
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }

    static void writeJson(Path outputPath, Object data) throws IOException {
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.writeString(outputPath, toPrettyJson(data), StandardCharsets.UTF_8);
    }

    static String normalizeText(String value) {
        String normalized = value == null ? "" : value.trim();
        if ("[deleted]".equalsIgnoreCase(normalized)
                || "[removed]".equalsIgnoreCase(normalized)
                || "null".equalsIgnoreCase(normalized)) {
            return "";
        }
        return normalized;
    }

    static String stripThingPrefix(String value) {
        int separator = value.indexOf('_');
        return separator >= 0 ? value.substring(separator + 1) : value;
    }

    static String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    static int intValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }
        if (node.isNumber()) {
            return node.asInt();
        }
        try {
            return (int) Math.round(Double.parseDouble(node.asText("0")));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    static double doubleValue(JsonNode node) {
        Double value = optionalDoubleValue(node);
        return value != null ? value : 0.0;
    }

    static Double optionalDoubleValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        String value = node.asText("");
        if (value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static double round(double value) {
        return Math.round(value * 1_000_000d) / 1_000_000d;
    }

    static final class JsonLineReader implements AutoCloseable {
        private final BufferedReader reader;

        JsonLineReader(Path path) throws IOException {
            this.reader = new BufferedReader(new InputStreamReader(
                    new ZstdInputStream(Files.newInputStream(path)),
                    StandardCharsets.UTF_8));
        }

        JsonNode readNext() throws IOException {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    return OBJECT_MAPPER.readTree(line);
                } catch (JsonProcessingException ignored) {
                    // Skip malformed lines and continue streaming.
                }
            }
            return null;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
