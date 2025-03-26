package server.models;

public enum StatusCode {
    NOT_FOUND(404),
    BAD_REQUEST(400),
    INTERNAL_SERVER_ERROR(500),

    NOK(999),

    WRONG_PWD(600),
    OK_NEW_USER(601),
    OK_USER(602),
    OK(603),
    NOPERM(604),
    NOWS(605),
    NOUSER(606)

    ;

    private final int code;

    StatusCode(int code) {
        this.code = code;
    }

    /**
     * Returns the status code.
     *
     * @return the status code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the status code from the code.
     *
     * @param code the code
     * @return the status code
     */
    public static StatusCode fromCode(int code) {
        for (StatusCode statusCode : StatusCode.values()) {
            if (statusCode.getCode() == code) {
                return statusCode;
            }
        }

        return null;
    }
}