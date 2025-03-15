package server.models;

import server.exceptions.InvalidRequestException;
import server.utils.JSONParser;
import server.utils.NetworkUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a request.
 */
public class Request {
    private final String uuid;
    private final BodyFormat format;
    private final String route;
    private final Body body;
    private final Map<String, String> headers;

    /**
     * Creates a new request.
     *
     * @param uuid Unique request ID
     * @param format Format type ("json" or "raw")
     * @param route Request route (e.g., "getworkspacefiles")
     * @param body Request body (JSON or raw)
     */
    public Request(String uuid, BodyFormat format, String route, Body body) {
        if (uuid == null || uuid.length() != RequestConstants.UUID_LENGTH) {
            throw new InvalidRequestException("UUID inválido: " + uuid);
        }
        // Regex: A rota só pode conter letras e números
        if (route == null || route.isBlank() || !route.matches("^[a-zA-Z0-9]+$")) {
            throw new InvalidRequestException("Rota inválida: " + route);
        }
        if (body == null) {
            throw new InvalidRequestException("Corpo inválido");
        }

        this.uuid = uuid;
        this.format = format;
        this.route = route;
        this.body = body;
        this.headers = new HashMap<>();
    }

    /**
     * Adds a header to the request.
     *
     * @param key Header key
     * @param value Header value
     * @return this request for chaining
     */
    public Request addHeader(String key, String value) {
        if (key.isBlank() || value.isBlank() || !isValidHeader(key) || !isValidHeader(value)) {
            throw new InvalidRequestException("Header inválido");
        }
        // replace if already exists
        headers.put(key, value);

        return this;
    }

    /**
     * Returns the size.
     *
     * @return the size
     */
    public int getSize() {
        int headersSize = 0;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            // Para cada: "KEY: VALUE\n"
            headersSize += entry.getKey().length() + 2 + entry.getValue().length() + 1;
        }

        return RequestConstants.HEADER_LENGTH + headersSize +
                route.getBytes(StandardCharsets.UTF_8).length + 2 + body.getSize();
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
     * Get the request format
     *
     * @return the request format
     */
    public BodyFormat getFormat() {
        return format;
    }

    /**
     * Returns the route.
     *
     * @return the route
     */
    public String getRoute() {
        return route;
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
     * Reads a request from the input stream.
     *
     * @param in the input stream
     * @return the request
     * @throws IOException if an I/O error occurs
     */
    public static Request fromStream(DataInputStream in) throws IOException {
        byte[] sizeBytes = new byte[RequestConstants.SIZE_LENGTH];
        in.readFully(sizeBytes);
        int size = NetworkUtils.byteArrayToInt(sizeBytes);

        byte[] uuidBytes = new byte[RequestConstants.UUID_LENGTH];
        in.readFully(uuidBytes);
        String uuid = new String(uuidBytes, StandardCharsets.UTF_8).trim();

        byte[] formatBytes = new byte[RequestConstants.FORMAT_LENGTH];
        in.readFully(formatBytes);
        BodyFormat format = BodyFormat.fromString(new String(formatBytes, StandardCharsets.UTF_8).trim());

        byte[] contentBytes = new byte[size - RequestConstants.HEADER_LENGTH];
        in.readFully(contentBytes);

        int separatorPos = findSeparatorPosition(contentBytes);
        if (separatorPos == -1) {
            throw new InvalidRequestException("Formato inválido: separador não encontrado");
        }

        // Headers e a rota estão antes do separador
        String headersAndRoute = new String(contentBytes, 0, separatorPos, StandardCharsets.UTF_8);
        String[] lines = headersAndRoute.split("\n");

        // A ultima linha sem : é a rota
        String route = "";
        Map<String, String> headers = new HashMap<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int colonIndex = line.indexOf(":");

            if (colonIndex > 0) { // header
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            } else { // rota
                route = line;
            }
        }

        Body body;
        // Extrai o corpo da requisição com base no formato
        if (format == BodyFormat.JSON) {
            int bodyStart = separatorPos + 2;
            String bodyJson = new String(contentBytes, bodyStart, contentBytes.length - bodyStart, StandardCharsets.UTF_8);
            body = BodyJSON.fromMap(JSONParser.deserialize(bodyJson));
        } else if (format == BodyFormat.RAW) {
            int bodyStart = separatorPos + 2;
            int bodyLength = contentBytes.length - bodyStart;
            byte[] bodyData = new byte[bodyLength];
            System.arraycopy(contentBytes, bodyStart, bodyData, 0, bodyLength);
            body = new BodyRaw(bodyData);
        } else {
            throw new InvalidRequestException("Formato inválido: " + format);
        }

        Request request = new Request(uuid, format, route, body);

        // Adiciona os headers ao request
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.addHeader(header.getKey(), header.getValue());
        }

        return request;
    }

    /**
     * Converts the request to a byte array for transmission.
     *
     * @return Byte array representing the request
     */
    public byte[] toByteArray() {
        byte[] bodyBytes;

        try {
            if (format == BodyFormat.JSON) {
                bodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);
            } else if (format == BodyFormat.RAW) {
                bodyBytes = ((BodyRaw) body).toBytes();
            } else {
                throw new InvalidRequestException("Formato inválido: " + format);
            }
        } catch (ClassCastException e) {
            throw new InvalidRequestException("Corpo inválido: " + body);
        }

        // Cria uma string com os headers
        StringBuilder headersBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headersBuilder.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        }

        String routePart = headersBuilder.toString() + route + "\n\n";
        byte[] routeBytes = routePart.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(this.getSize());
        buffer.putInt(this.getSize()); // Size
        buffer.put(uuid.getBytes(StandardCharsets.UTF_8)); // UUID
        buffer.put(format.toString().getBytes(StandardCharsets.UTF_8)); // Format
        buffer.put(routeBytes); // Headers + Route com separadores
        buffer.put(bodyBytes); // Body

        return buffer.array();
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

    @Override
    public String toString() {
        int size = this.getSize();

        StringBuilder sb = new StringBuilder();
        sb.append("Request{\n");
        sb.append("    \"size\": ").append(size).append("\n");
        sb.append("    \"uuid\": \"").append(uuid).append("\"\n");
        sb.append("    \"format\": ").append(format).append("\n");
        sb.append("    \"route\": \"").append(route).append("\"\n");

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

//REQUEST
//-------------------------------------------
//size[4 bytes] uuid[4 bytes] format[4 bytes]
//HEADER-1: VALUE1
//HEADER-2: VALUE2
///getworkspacefiles
//
//{
//    "name": "Nome"
//}
//-------------------------------------------