package server.models;

/**
 * Represents a raw binary body for a Request or Response.
 */
public class BodyRaw extends Body {
    private final byte[] data;

    /**
     * Creates an empty raw body.
     */
    public BodyRaw() {
        this.data = new byte[0];
    }

    /**
     * Creates a raw body with binary data.
     *
     * @param data the binary data
     */
    public BodyRaw(byte[] data) {
        this.data = data.clone();
    }

    /**
     * Returns the binary data.
     *
     * @return the binary data
     */
    public byte[] toBytes() {
        return data.clone();
    }

    /**
     * Converts a byte array into a BodyRaw object.
     *
     * @param data the byte array
     * @return a BodyRaw object
     */
    public static BodyRaw fromBytes(byte[] data) {
        return new BodyRaw(data);
    }

    /**
     * Returns the size of the raw body in bytes.
     *
     * @return The size in bytes.
     */
    @Override
    public int getSize() {
        return data.length;
    }

    @Override
    public BodyFormat getFormat() {
        return BodyFormat.RAW;
    }
}
