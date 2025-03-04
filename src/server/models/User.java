package server.models;

public class User {
    private final String userId;
    private final String password;

    /**
     * Create a new user.
     *
     * @param userId the user ID
     * @param password the user password
     */
    public User(String userId, String password) {
        this.userId = userId;
        this.password = password;
    }

    /**
     * Get the user ID.
     *
     * @return the user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Get the user password.
     *
     * @return the user password
     */
    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return userId + ":" + password;
    }
}
