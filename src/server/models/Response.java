package server.models;

import server.exceptions.InvalidResponseException;
import server.utils.JSONParser;
import server.utils.NetworkUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Represents a response.
 */
public class Response {
    private final String uuid;
    private final BodyFormat format;
    private final int status;
    private final Body body;

    /**
     * Creates a new response.
     *
     * @param uuid Unique request ID
     * @param format Format type ("json" or "raw")
     * @param status Response status code
     * @param body Response body (JSON or raw)
     */
    public Response(String uuid, BodyFormat format, int status, Body body) {
        if (uuid == null || uuid.length() != RequestConstants.UUID_LENGTH) {
            throw new InvalidResponseException("UUID inválido: " + uuid);
        }
        if (status < 100 || status > 599) {
            throw new InvalidResponseException("Status inválido: " + status);
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
    }

    /**
     * Returns the size.
     *
     * @return the size
     */
    public int getSize() {
        return RequestConstants.HEADER_LENGTH + (status + "\n\n").getBytes(StandardCharsets.UTF_8).length + body.getSize();
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
    public int getStatus() {
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
     * Reads a response from the input stream.
     *
     * @param in the input stream
     * @return the response
     * @throws IOException if an I/O error occurs
     */
    public static Response fromStream(DataInputStream in) throws IOException {
        byte[] sizeBytes = new byte[RequestConstants.SIZE_LENGTH];
        in.readFully(sizeBytes);
        int size = NetworkUtils.byteArrayToInt(sizeBytes);

        byte[] uuidBytes = new byte[RequestConstants.UUID_LENGTH];
        in.readFully(uuidBytes);
        String uuid = new String(uuidBytes, StandardCharsets.UTF_8).trim();

        byte[] formatBytes = new byte[RequestConstants.FORMAT_LENGTH];
        in.readFully(formatBytes);
        BodyFormat format = BodyFormat.fromString(new String(formatBytes, StandardCharsets.UTF_8).trim());

        byte[] statusBytes = new byte[size - RequestConstants.HEADER_LENGTH];
        in.readFully(statusBytes);
        String rawResponse = new String(statusBytes, StandardCharsets.UTF_8).trim();

        String[] parts = rawResponse.split("\n\n", 2);
        if (parts.length != 2) {
            throw new InvalidResponseException("Response inválida: " + rawResponse);
        }

        int status = Integer.parseInt(parts[0].trim());
        Body body = BodyJSON.fromMap(JSONParser.deserialize(parts[1]));

        return new Response(uuid, format, status, body);
    }

    /**
     * Converts the response to a byte array for transmission.
     *
     * @return Byte array representing the response
     */
    public byte[] toByteArray() {
        String rawResponse = this.status + "\n\n" + this.body.toString();
        byte[] responseBodyBytes = rawResponse.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(getSize());
        buffer.putInt(getSize()); // Size
        buffer.put(uuid.getBytes(StandardCharsets.UTF_8)); // UUID
        buffer.put(format.toString().getBytes(StandardCharsets.UTF_8)); // Format
        buffer.put(responseBodyBytes); // Body

        return buffer.array();
    }

    @Override
    public String toString() {
        int size = getSize();

//        return "Response{" +
//                "size=" + size +
//                ", uuid='" + uuid + '\'' +
//                ", format=" + format +
//                ", status=" + status +
//                ", body=" + body +
//                '}';

        return "Response{\n" +
                "    \"size\": " + size + "\n" +
                "    \"uuid\": \"" + uuid + "\"\n" +
                "    \"format\": " + format + "\n" +
                "    \"status\": " + status + "\n" +
                "    \"body\": " + body + "\n" +
                "}";
    }
}

//RESPONSE
//-----------------------------------------
//size[4 bytes] id[x bytes] format[x bytes]
//
//200
//
//{
//"name": "Nome"
//}
//-----------------------------------------



