package server;

import server.models.User;

import java.util.Map;

public class AuthenticationManager {
    private static AuthenticationManager INSTANCE;
    private static final String USERS_FILE_PATH = "users.txt";
    private final UserStorageManager userStorageManager;
    private Map<String, String> users;

    /**
     * Get the instance of the authentication manager.
     */
    private AuthenticationManager() {
        this.userStorageManager = new UserStorageManager(USERS_FILE_PATH);
        this.users = userStorageManager.loadUsers();
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
        if (!users.containsKey(userId)) {
            if (this.addUser(userId, password)) {
                return ServerResponse.OK;
            } else {
                return ServerResponse.ERROR;
            }
        } else if (this.checkAuth(userId, password)) {
            return ServerResponse.OK;
        } else {
            return ServerResponse.INVALID_AUTH;
        }
    }

    public boolean addUser(String userId, String password) {
        if (this.userStorageManager.addUser(userId, password)) {
            this.users = userStorageManager.loadUsers();
            return true;
        }

        return false;
    }

    public boolean checkAuth(String user, String passwd) {
        return users.containsKey(user) && users.get(user).equals(passwd);
    }

    public User getUser(String userId) {
        return new User(userId, users.get(userId));
    }
}
