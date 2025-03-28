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
        return isSimpleFile(input);
        //        return isSimpleFile(input) || isFileInDirectory(input) || isDirectoryPath(input);
    }

    private static boolean isSimpleFile(String input) {
        if (input == null) {
            return false;
        }

        // Matches: filename.ext or filename (without extension)
        return input.matches("^[a-zA-Z0-9]*(\\.[a-zA-Z0-9]+)?$");
    }

    // Removed for security reasons
//    private static boolean isFileInDirectory(String input) {
//        // Matches: dir/file.ext or dir/subdir/file.ext
//        return input.matches("^[a-zA-Z0-9]+(/[a-zA-Z0-9]+)+\\.[a-zA-Z0-9]+$");
//    }
//
//    private static boolean isDirectoryPath(String input) {
//        // Matches: dir/subdir or dir/subdir/subdir2
//        return input.matches("^[a-zA-Z0-9]+(/[a-zA-Z0-9]+)+$");
//    }

    /**
     * Check if the workspace id is valid.
     *
     * @param input the workspace id
     * @return true if the workspace id is valid, false otherwise
     */
    public static boolean isValidWorkspaceId(String input) {
        if (input == null) {
            return false;
        }
        if (input.isBlank()) {
            return false;
        }

        return input.matches("^[a-zA-Z0-9_]*$");
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
