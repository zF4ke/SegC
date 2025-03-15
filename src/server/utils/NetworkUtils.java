package server.utils;

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
}
