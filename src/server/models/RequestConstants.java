package server.models;

public class RequestConstants {
    public static final int SIZE_LENGTH = 4;     // Size is an integer (4 bytes)
    public static final int UUID_LENGTH = 36;    // UUID string (36 chars)
    public static final int FORMAT_LENGTH = 4;    // "json" or "raw" (4 chars)

    // Header size: size + uuid + format
    public static final int HEADER_LENGTH = SIZE_LENGTH + UUID_LENGTH + FORMAT_LENGTH;
}