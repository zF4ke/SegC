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
     * Treats all values as simple strings without interpreting nested structures.
     *
     * @param json the JSON-formatted string
     * @return the map
     */
    public static Map<String, String> deserialize(String json) {
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return Map.of();
        }

        Map<String, String> result = new HashMap<>();
        String content = json.substring(1, json.length() - 1).trim();

        int i = 0;
        while (i < content.length()) {
            // Skip leading whitespace
            while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
                i++;
            }

            // Find key start (first quote)
            if (i >= content.length() || content.charAt(i) != '"') break;
            int keyStart = i;

            // Find key end (closing quote)
            i = findClosingQuote(content, keyStart + 1);
            if (i == -1) break;
            String key = unescape(content.substring(keyStart + 1, i));
            i++;

            // Find colon
            while (i < content.length() && content.charAt(i) != ':') {
                i++;
            }
            if (i >= content.length()) break;
            i++;

            // Skip whitespace after colon
            while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
                i++;
            }

            // Find value start
            if (i >= content.length() || content.charAt(i) != '"') break;
            int valueStart = i;

            // Find value end
            i = findClosingQuote(content, valueStart + 1);
            if (i == -1) break;
            String value = unescape(content.substring(valueStart + 1, i));
            i++;

            result.put(key, value);

            // Find next pair
            while (i < content.length() && content.charAt(i) != ',') {
                i++;
            }
            if (i < content.length()) i++;
        }

        return result;
    }

    /**
     * Finds the closing quote of a quoted string.
     *
     * @param text the text
     * @param start the index of the opening quote
     * @return the index of the closing quote, or -1 if not found
     */
    private static int findClosingQuote(String text, int start) {
        for (int i = start; i < text.length(); i++) {
            // Found an unescaped quote
            if (text.charAt(i) == '"' && (i == 0 || text.charAt(i-1) != '\\')) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Escapes special characters in a string.
     *
     * @param text the string
     * @return the escaped string
     */
    private static String escape(String text) {
        if (text == null) return "";
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
