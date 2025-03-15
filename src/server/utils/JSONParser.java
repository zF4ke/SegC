package server.utils;

import java.util.HashMap;
import java.util.Map;

public class JSONParser {

    /**
     * Serializes a map to a JSON-formatted string.
     *
     * @param data the map
     * @return a JSON-formatted string
     */
    public static String serialize(Map<String, String> data) {
        // Se estiver vazio
        if (data == null || data.isEmpty()) {
            return "{}";
        }

        StringBuilder result = new StringBuilder("{");
        boolean first = true;

        // Itera sobre o mapa
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!first) {
                result.append(",");
            }
            result.append("\"")
                    .append(escape(entry.getKey()))
                    .append("\":\"")
                    .append(escape(entry.getValue()))
                    .append("\"");
            first = false;
        }

        result.append("}");
        return result.toString();
    }

    /**
     * Deserializes a JSON-formatted string to a map.
     *
     * @param json the JSON-formatted string
     * @return the map
     */
    public static Map<String, String> deserialize(String json) {
        // Se estiver vazio
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return Map.of();
        }

        Map<String, String> result = new HashMap<>();
        // Remove as chaves {}
        String[] pairs = json.substring(1, json.length() - 1).split(",");

        // Itera sobre os pares
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;

            int colonPos = pair.indexOf(':');
            if (colonPos == -1) throw new IllegalArgumentException("Formato JSON inválido");

            // Pega a chave e o valor
            String key = pair.substring(pair.indexOf('"') + 1, pair.lastIndexOf('"', colonPos));
            String value = pair.substring(pair.indexOf('"', colonPos) + 1, pair.lastIndexOf('"'));

            result.put(unescape(key), unescape(value));
        }

        return result;
    }

    /**
     * Escapes special characters in a string.
     *
     * @param text the string
     * @return the escaped string
     */
    private static String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    /**
     * Unescapes special characters in a string.
     *
     * @param text the string
     * @return the unescaped string
     */
    private static String unescape(String text) {
        return text.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\b", "\b")
                .replace("\\f", "\f");
    }
}
