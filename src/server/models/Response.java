package server.models;

import server.exceptions.InvalidResponseException;
import server.utils.JSONParser;
import server.utils.NetworkUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a response.
 */
public class Response {
    private final String uuid;
    private final BodyFormat format;
    private final StatusCode status;
    private final Body body;
    private final Map<String, String> headers;

    /**
     * Creates a new response.
     *
     * @param uuid Unique request ID
     * @param status Response status code
     * @param format Format type ("json" or "raw")
     * @param body Response body (JSON or raw)
     */
    public Response(String uuid, StatusCode status, BodyFormat format, Body body) {
        if (uuid == null || uuid.length() != RequestConstants.UUID_LENGTH) {
            throw new InvalidResponseException("UUID inválido: " + uuid);
        }
        if (status == null) {
            throw new InvalidResponseException("Status inválido");
        }
        if (body == null) {
            if (format == BodyFormat.JSON) {
                body = new BodyJSON();
            } else {
                body = new BodyRaw();
            }
        }

        this.uuid = uuid;
        this.format = format;
        this.status = status;
        this.body = body;
        this.headers = new HashMap<>();
    }

    /**
     * Returns the size.
     *
     * @return the size
     */
    public int getSize() {
        int headersSize = 0;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            // For each: "KEY: VALUE\n"
            headersSize += entry.getKey().length() + 2 + entry.getValue().length() + 1;
        }

        // Size of the fixed-length header components
        int headerSize = 4 + // size field (4 bytes)
                RequestConstants.UUID_LENGTH + // UUID
                RequestConstants.FORMAT_LENGTH; // Format

        // Size of the status code string + double newline + body
        String statusStr = this.status.getCode() + "\n\n";
        int statusSize = statusStr.getBytes(StandardCharsets.UTF_8).length;

        return headerSize + headersSize + statusSize + body.getSize();
    }


    /**
     * Adds a header to the response.
     *
     * @param key Header key
     * @param value Header value
     * @return this response for chaining
     */
    public Response addHeader(String key, String value) {
        if (key.isBlank() || value.isBlank() || !isValidHeader(key) || !isValidHeader(value)) {
            throw new InvalidResponseException("Header inválido");
        }
        // replace if already exists
        headers.put(key, value);

        return this;
    }

    /**
     * Gets a header value.
     *
     * @param key Header key
     * @return Header value or null if not found
     */
    public String getHeader(String key) {
        return headers.get(key);
    }

    /**
     * Checks if a header exists.
     *
     * @param key Header key
     * @return true if the header exists, false otherwise
     */
    public boolean hasHeader(String key) {
        return headers.containsKey(key);
    }

    /**
     * Gets all headers.
     *
     * @return Map of headers
     */
    public Map<String, String> getHeaders() {
        return Map.copyOf(headers);  // Return an unmodifiable copy
    }

    /**
     * Returns the UUID.
     *
     * @return the UUID
     */
    public String getUUID() {
        return uuid;
    }

    /**
     * Returns the format.
     *
     * @return the format
     */
    public BodyFormat getFormat() {
        return format;
    }

    /**
     * Returns the status.
     *
     * @return the status
     */
    public StatusCode getStatus() {
        return status;
    }

    /**
     * Returns the body.
     *
     * @return the body
     */
    public Body getBody() {
        return body;
    }

    /**
     * Returns the body as a BodyJSON if it's in JSON format.
     *
     * @return The body as a BodyJSON
     * @throws IllegalArgumentException if the body format is not JSON
     */
    public BodyJSON getBodyJSON() throws IllegalArgumentException {
        if (this.getBody().getFormat() != BodyFormat.JSON || !(this.getBody() instanceof BodyJSON)) {
            throw new IllegalArgumentException("O corpo não está no formato JSON");
        }
        return (BodyJSON) this.getBody();
    }

    /**
     * Returns the body as a BodyRaw if it's in RAW format.
     *
     * @return The body as a BodyRaw
     * @throws IllegalArgumentException if the body format is not RAW
     */
    public BodyRaw getBodyRaw() throws IllegalArgumentException {
        if (this.getBody().getFormat() != BodyFormat.RAW || !(this.getBody() instanceof BodyRaw)) {
            throw new IllegalArgumentException("O corpo não está no formato RAW");
        }
        return (BodyRaw) this.getBody();
    }

    /**
     * Reads a response from the input stream.
     *
     * @param in the input stream
     * @return the response
     * @throws IOException if an I/O error occurs
     */
    public static Response fromStream(DataInputStream in) throws IOException {
        // Read size (first 4 bytes)
        int size = in.readInt();

        // Read uuid (36 bytes)
        byte[] uuidBytes = new byte[RequestConstants.UUID_LENGTH];
        in.readFully(uuidBytes);
        String uuid = new String(uuidBytes, StandardCharsets.UTF_8);

        // Read format
        byte[] formatBytes = new byte[RequestConstants.FORMAT_LENGTH];
        in.readFully(formatBytes);
        String formatStr = new String(formatBytes, StandardCharsets.UTF_8);
        BodyFormat format = BodyFormat.fromString(formatStr);

        // Calculate body content size
        int headerSize = 4 + RequestConstants.UUID_LENGTH + RequestConstants.FORMAT_LENGTH;
        int contentSize = size - headerSize;

        // Read the content (headers, status and body)
        byte[] contentBytes = new byte[contentSize];
        in.readFully(contentBytes);

        int separatorPos = findSeparatorPosition(contentBytes);
        if (separatorPos == -1) {
            throw new InvalidResponseException("Formato inválido: separador não encontrado");
        }

        // Headers and status are before the separator
        String headersAndStatus = new String(contentBytes, 0, separatorPos, StandardCharsets.UTF_8);
        String[] lines = headersAndStatus.split("\n");

        // The last line without a colon is the status
        String statusStr = "";
        Map<String, String> headers = new HashMap<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int colonIndex = line.indexOf(":");

            if (colonIndex > 0) { // header
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            } else { // status
                statusStr = line;
            }
        }

        StatusCode status = StatusCode.fromCode(Integer.parseInt(statusStr));

        // Extract body based on format
        Body body;
        int bodyStart = separatorPos + 2;
        if (format == BodyFormat.JSON) {
            String bodyJson = new String(contentBytes, bodyStart, contentBytes.length - bodyStart, StandardCharsets.UTF_8);
            body = BodyJSON.fromMap(JSONParser.deserialize(bodyJson));
        } else if (format == BodyFormat.RAW) {
            int bodyLength = contentBytes.length - bodyStart;
            byte[] bodyData = new byte[bodyLength];
            System.arraycopy(contentBytes, bodyStart, bodyData, 0, bodyLength);
            body = new BodyRaw(bodyData);
        } else {
            throw new InvalidResponseException("Formato inválido: " + format);
        }

        Response response = new Response(uuid, status, format, body);

        // Add headers to response
        for (Map.Entry<String, String> header : headers.entrySet()) {
            response.addHeader(header.getKey(), header.getValue());
        }

        return response;
    }

    /**
     * Find the position of the separator "\n\n" in a byte array.
     *
     * @param bytes the byte array
     * @return the position of the separator, or -1 if not found
     */
    private static int findSeparatorPosition(byte[] bytes) {
        // Look for "\n\n" sequence
        for (int i = 0; i < bytes.length - 1; i++) {
            if (bytes[i] == '\n' && bytes[i+1] == '\n') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks if a string is a valid header key or value.
     *
     * @param s the string
     * @return true if valid, false otherwise
     */
    private boolean isValidHeader(String s) {
        return s.matches("^[a-zA-Z0-9-]+$");
    }

    /**
     * Converts the response to a byte array for transmission.
     *
     * @return Byte array representing the response
     */
    public byte[] toByteArray() {
        byte[] bodyBytes;

        try {
            if (format == BodyFormat.JSON) {
                bodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);
            } else if (format == BodyFormat.RAW) {
                bodyBytes = ((BodyRaw) body).toBytes();
            } else {
                throw new InvalidResponseException("Formato inválido: " + format);
            }
        } catch (ClassCastException e) {
            throw new InvalidResponseException("Corpo inválido: " + body);
        }

        // Create a string with the headers
        StringBuilder headersBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headersBuilder.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        }

        String statusPart = headersBuilder.toString() + status.getCode() + "\n\n";
        byte[] statusBytes = statusPart.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(getSize());
        buffer.putInt(getSize()); // Size
        buffer.put(uuid.getBytes(StandardCharsets.UTF_8)); // UUID
        buffer.put(format.toString().getBytes(StandardCharsets.UTF_8)); // Format
        buffer.put(statusBytes); // Headers + Status with separators
        buffer.put(bodyBytes); // Body

        return buffer.array();
    }

    @Override
    public String toString() {
        int size = getSize();

        StringBuilder sb = new StringBuilder();
        sb.append("Response{\n");
        sb.append("    \"size\": ").append(size).append("\n");
        sb.append("    \"uuid\": \"").append(uuid).append("\"\n");
        sb.append("    \"format\": ").append(format).append("\n");
        sb.append("    \"status\": ").append(status).append("\n");

        if (!headers.isEmpty()) {
            sb.append("    \"headers\": {\n");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                sb.append("        \"").append(entry.getKey()).append("\": \"")
                        .append(entry.getValue()).append("\",\n");
            }
            sb.append("    },\n");
        }

        sb.append("    \"body\": ").append(body).append("\n");
        sb.append("}");

        return sb.toString();
    }
}

//RESPONSE
//-----------------------------------------
//size[4 bytes] id[x bytes] format[x bytes]
//HEADER=VALUE
//200
//
//{
//"name": "Nome"
//}
//-----------------------------------------



