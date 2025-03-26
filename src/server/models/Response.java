package server.models;

import server.exceptions.InvalidResponseException;
import server.utils.JSONParser;
import server.utils.NetworkUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Represents a response.
 */
public class Response {
    private final String uuid;
    private final BodyFormat format;
    private final StatusCode status;
    private final Body body;

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
    }

    /**
     * Returns the size.
     *
     * @return the size
     */
    public int getSize() {
        // Size of the fixed-length header components
        int headerSize = 4 + // size field (4 bytes)
                RequestConstants.UUID_LENGTH + // UUID
                RequestConstants.FORMAT_LENGTH; // Format

        // Size of the status code string + double newline + body
        String statusStr = this.status.getCode() + "\n\n";
        int bodyContentSize = statusStr.getBytes(StandardCharsets.UTF_8).length + body.getSize();

        return headerSize + bodyContentSize;
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
        int bodyContentSize = size - headerSize;

        // Read the status and body
        byte[] bodyContentBytes = new byte[bodyContentSize];
        in.readFully(bodyContentBytes);
        String bodyContent = new String(bodyContentBytes, StandardCharsets.UTF_8);

        // Split status and body content (separated by double newline)
        int separatorIndex = bodyContent.indexOf("\n\n");
        if (separatorIndex < 0) {
            throw new InvalidResponseException("Invalid response format: missing separator");
        }

        String statusStr = bodyContent.substring(0, separatorIndex);
        StatusCode status = StatusCode.fromCode(Integer.parseInt(statusStr));

        String bodyStr = "";
        if (separatorIndex + 2 < bodyContent.length()) {
            bodyStr = bodyContent.substring(separatorIndex + 2);
        }

        Body body;
        if (format == BodyFormat.JSON) {
            body = new BodyJSON(JSONParser.deserialize(bodyStr));
        } else {
            body = new BodyRaw(bodyStr.getBytes(StandardCharsets.UTF_8));
        }

        return new Response(uuid, status, format, body);
    }

    /**
     * Converts the response to a byte array for transmission.
     *
     * @return Byte array representing the response
     */
    public byte[] toByteArray() {
        String rawResponse = this.status.getCode() + "\n\n" + this.body.toString();
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



