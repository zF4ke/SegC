package server.utils;

public class InputUtils {
    /**
     * Check if the input is alphanumeric.
     *
     * @param input the input
     * @return true if the input is alphanumeric, false otherwise
     */
    public static boolean isAlfaNumeric(String input) {
        return input.matches("^[a-zA-Z0-9]*$");
    }

    /**
     * Check if the filename is valid.
     *
     * @param input the filename
     * @return true if the filename is valid, false otherwise
     */
    public static boolean isValidFilename(String input) {
        return input.matches("^[a-zA-Z0-9]+\\.[a-zA-Z]+$");
    }

    /**
     * Check if the workspace id is valid.
     *
     * @param input the workspace id
     * @return true if the workspace id is valid, false otherwise
     */
    public static boolean isValidWorkspaceId(String input) {
        return input.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * Check if the user id is valid.
     *
     * @param input the user id
     * @return true if the user id is valid, false otherwise
     */
    public static boolean isValidUserId(String input) {
        return input.matches("^[a-zA-Z0-9]+$");
    }

    /**
     * Check if the username and password are valid.
     *
     * @param username the username
     * @param password the password
     * @return true if the username and password are valid, false otherwise
     */
    public static boolean isValidUsernameAndPassword(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        if (username.isBlank() || password.isBlank()) {
            return false;
        }

        return isAlfaNumeric(username) && isAlfaNumeric(password);
    }

}
