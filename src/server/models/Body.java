package server.models;

public abstract class Body {
    /**
     * Returns the size of the body in bytes.
     *
     * @return The size in bytes.
     */
    public abstract int getSize();

    /**
     * Returns the body format.
     *
     * @return The body format.
     */
    public abstract BodyFormat getFormat();
}