package server.utils;

import server.models.*;

public class NetworkUtils {
    /**
     * Convert byte array to integer.
     *
     * @param arr byte array
     * @return integer
     */
    public static int byteArrayToInt(byte[] arr) {
        return ((arr[0] & 0xFF) << 24) | ((arr[1] & 0xFF) << 16) | ((arr[2] & 0xFF) << 8) | (arr[3] & 0xFF);
    }

    /**
     * Convert integer to byte array.
     *
     * @param value integer
     * @return byte array
     */
    public static byte[] intToByteArray(int value) {
        return new byte[] {
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) value
        };
    }

    /**
     * Generate a random UUID.
     *
     * @return the UUID
     */
    public static String randomUUID() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Creates a response with an error message.
     *
     * @param request the request
     * @param errorMessage the error message
     * @return the response
     */
    public static Response createErrorResponse(Request request, String errorMessage) {
        BodyJSON body = new BodyJSON();
        body.put("error", errorMessage);

        return new Response(
                request.getUUID(),
                StatusCode.BAD_REQUEST,
                BodyFormat.JSON,
                body
        );
    }

    /**
     * Creates a response with an error message.
     *
     * @param request the request
     * @param status the status code
     * @return the response
     */
    public static Response createErrorResponse(Request request, StatusCode status) {
        BodyJSON body = new BodyJSON();
        body.put("error", status.name());

        return new Response(
                request.getUUID(),
                status,
                BodyFormat.JSON,
                body
        );
    }
}
