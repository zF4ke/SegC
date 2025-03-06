package server;

import server.models.User;

import java.util.Map;

public class AuthenticationManager {
    private static AuthenticationManager INSTANCE;
    private static final String USERS_FILE_PATH = "users.txt";
    private final UserStorageManager userStorageManager;

    /**
     * Get the instance of the authentication manager.
     */
    private AuthenticationManager() {
        this.userStorageManager = new UserStorageManager(USERS_FILE_PATH);
    }

    /**
     * Get the singleton instance of AuthenticationManager.
     *
     * @return The singleton instance.
     */
    public static synchronized AuthenticationManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AuthenticationManager();
        }
        return INSTANCE;
    }

    /**
     * Authenticate a user.
     * If the user does not exist, it will be created and the ServerResponse will be OK.
     * If the user exists and the password is correct, the ServerResponse will be OK.
     * If the user exists and the password is incorrect, the ServerResponse will be INVALID_AUTH.
     *
     * @param userId the user ID
     * @param password the user password
     * @return the server response
     */
    public ServerResponse authenticate(String userId, String password) {
        if (this.getUser(userId) == null) {
            if (this.addUser(userId, password)) {
                return ServerResponse.OK;
            } else {
                return ServerResponse.ERROR;
            }
        } else if (this.checkPassword(userId, password)) {
            return ServerResponse.OK;
        } else {
            return ServerResponse.INVALID_AUTH;
        }
    }

    /**
     * Add a user to the storage.
     *
     * @param userId the user ID
     * @param password the user password
     * @return true if the user was added, false otherwise
     */
    public boolean addUser(String userId, String password) {
        return this.userStorageManager.addUser(userId, password);
    }

    /**
     * Check if the user password is correct.
     *
     * @param userId the user ID
     * @param passwd the user password
     * @return true if the password is correct, false otherwise
     */
    public boolean checkPassword(String userId, String passwd) {
        User user = this.getUser(userId);
        if (user == null) {
            return false;
        }

        return user.getPassword().equals(passwd);
    }

    /**
     * Get a user from the storage.
     *
     * @param userId the user ID
     * @return the user, or null if the user does not exist
     */
    public User getUser(String userId) {
        return this.userStorageManager.getUser(userId);
    }
}
