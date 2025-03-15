package server.models;

import server.utils.JSONParser;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a JSON-based body for a Request or Response.
 */
public class BodyJSON extends Body {
    private final Map<String, String> data;

    /**
     * Creates a new empty JSON body.
     */
    public BodyJSON() {
        this.data = new HashMap<>();
    }

    /**
     * Creates a new JSON body with the given data.
     *
     * @param data the JSON data
     */
    public BodyJSON(Map<String, String> data) {
        this.data = new HashMap<>(data);
    }

    /**
     * Returns the data.
     *
     * @return the JSON data
     */
    public Map<String, String> getData() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * Returns the value of the given key.
     *
     * @param key the key
     * @return the value
     */
    public String get(String key) {
        return data.get(key);
    }

    /**
     * Puts a new key-value pair in the JSON data.
     *
     * @param key the key
     * @param value the value
     */
    public void put(String key, String value) {
        data.put(key, value);
    }

    /**
     * Converts a map to a BodyJSON object.
     *
     * @param map the map
     * @return a BodyJSON object
     */
    public static BodyJSON fromMap(Map<String, String> map) {
        return new BodyJSON(map);
    }

    /**
     * Returns the JSON body as a string.
     *
     * @return A JSON-formatted string.
     */
    @Override
    public String toString() {
        return JSONParser.serialize(data);
    }

    /**
     * Returns the size of the JSON body in bytes.
     *
     * @return The size in bytes.
     */
    @Override
    public int getSize() {
        return toString().getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    public BodyFormat getFormat() {
        return BodyFormat.JSON;
    }

    /**
     * Checks if the JSON body contains the given key.
     *
     * @param message the key
     * @return true if the key exists, false otherwise
     */
    public boolean containsKey(String message) {
        return data.containsKey(message);
    }
}