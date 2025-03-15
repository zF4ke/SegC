package server.models;

public enum BodyFormat {
    JSON("json"),
    RAW("rawf");

    private final String format;

    BodyFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public static BodyFormat fromString(String format) {
        if (format == null) {
            throw new IllegalArgumentException("O formato não pode ser nulo");
        }

        for (BodyFormat f : BodyFormat.values()) {
            if (f.getFormat().equalsIgnoreCase(format.trim())) {
                return f;
            }
        }

        throw new IllegalArgumentException("Formato inválido: " + format);
    }

    @Override
    public String toString() {
        return format;
    }
}
