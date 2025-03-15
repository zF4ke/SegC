package server.models;

import server.exceptions.InvalidRequestException;
import server.utils.JSONParser;
import server.utils.NetworkUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Represents a request.
 */
public class Request {
    private final String uuid;
    private final BodyFormat format;
    private final String route;
    private final Body body;

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
        if (route == null || !route.matches("^[a-zA-Z0-9]+$") || route.isBlank()) {
            throw new InvalidRequestException("Rota inválida: " + route);
        }
        if (body == null) {
            throw new InvalidRequestException("Corpo inválido");
        }

        this.uuid = uuid;
        this.format = format;
        this.route = route;
        this.body = body;
    }

    /**
     * Returns the size.
     *
     * @return the size
     */
    public int getSize() {
        return RequestConstants.HEADER_LENGTH + route.getBytes(StandardCharsets.UTF_8).length + 2 + body.getSize();
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

        if (format == BodyFormat.JSON) {
            String rawRequest = new String(contentBytes, StandardCharsets.UTF_8).trim();
            String[] parts = rawRequest.split("\n\n", 2);

            if (parts.length != 2) {
                throw new InvalidRequestException("Corpo inválido: " + rawRequest);
            }

            String route = parts[0].trim();
            Body body = BodyJSON.fromMap(JSONParser.deserialize(parts[1]));
            return new Request(uuid, format, route, body);
        } else if (format == BodyFormat.RAW) {
            // Para o formato RAW, encontra a primeira dupla quebra de linha para separar a rota dos dados binários
            int separatorPos = findSeparatorPosition(contentBytes);
            if (separatorPos == -1) {
                throw new InvalidRequestException("Formato RAW inválido: separador não encontrado");
            }

            // Extrai a rota (converte apenas a parte da rota para String)
            String route = new String(contentBytes, 0, separatorPos, StandardCharsets.UTF_8).trim();

            // Extrai a parte do corpo (dados binários) a partir da posição do separador
            int bodyStart = separatorPos + 2;
            int bodyLength = contentBytes.length - bodyStart;
            byte[] bodyData = new byte[bodyLength];
            System.arraycopy(contentBytes, bodyStart, bodyData, 0, bodyLength);

            Body body = new BodyRaw(bodyData);
            return new Request(uuid, format, route, body);
        }
        else {
            throw new InvalidRequestException("Formato inválido: " + format);
        }
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

        String routePart = this.route + "\n\n";
        byte[] routeBytes = routePart.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(this.getSize());
        buffer.putInt(this.getSize()); // Size
        buffer.put(uuid.getBytes(StandardCharsets.UTF_8)); // UUID
        buffer.put(format.toString().getBytes(StandardCharsets.UTF_8)); // Format
        buffer.put(routeBytes); // Route with separators
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


    @Override
    public String toString() {
        int size = this.getSize();

//        return "Request{" +
//                "size=" + size +
//                ", uuid='" + uuid + '\'' +
//                ", format=" + format +
//                ", route='" + route + '\'' +
//                ", body=" + body +
//                '}';

        return "Request{\n" +
                "    \"size\": " + size + "\n" +
                "    \"uuid\": \"" + uuid + "\"\n" +
                "    \"format\": " + format + "\n" +
                "    \"route\": \"" + route + "\"\n" +
                "    \"body\": " + body + "\n" +
                "}";
    }
}

//REQUEST
//-------------------------------------------
//size[4 bytes] uuid[4 bytes] format[4 bytes]
//
///getworkspacefiles
//
//{
//    "name": "Nome"
//}
//-------------------------------------------