package server;

import server.models.StatusCode;
import server.models.User;

public class AuthenticationManager {
    public static AuthenticationManager INSTANCE;
    private final UserStorageManager userStorageManager;

    private final String DEFAULT_WORKSPACE_NAME = "workspace001";

    /**
     * Create a new authentication manager.
     */
    private AuthenticationManager() {
        this.userStorageManager = UserStorageManager.getInstance();
    }

    /**
     * Get the instance of the authentication manager.
     *
     * @return the instance
     */
    public synchronized static AuthenticationManager getInstance() {
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
    public StatusCode authenticate(String userId, String password) {
        if (this.getUser(userId) == null) {
            if (this.addUser(userId, password)) {
                WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
                StatusCode status = workspaceManager.createWorkspace(userId, DEFAULT_WORKSPACE_NAME);

                if (status == StatusCode.OK) {
                    return StatusCode.OK_NEW_USER;
                } else {
                    return StatusCode.INTERNAL_SERVER_ERROR;
                }
            }

            return StatusCode.INTERNAL_SERVER_ERROR;
        } else if (this.checkPassword(userId, password)) {
            return StatusCode.OK_USER;
        } else {
            return StatusCode.WRONG_PWD;
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
     * Remove a user from the storage.
     *
     * @param userId the user ID
     * @return true if the user was removed, false otherwise
     */
    public boolean removeUser(String userId) {
        return this.userStorageManager.removeUser(userId);
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
