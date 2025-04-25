package server.models;

public class User {
    private final String userId;
    private final String hash;
    private final String salt;

    /**
     * Create a new user.
     *
     * @param userId the user ID
     * @param hash the user password+salt hash
     * @param salt the user password salt
     */
    public User(String userId, String hash, String salt) {
        this.userId = userId;
        this.hash = hash;
        this.salt = salt;
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
     * Get the user hashed password+salt.
     *
     * @return the user hashed password+salt
     */
    public String getHash() {
        return hash;
    }

    /**
     * Get the user password salt.
     *
     * @return the user password salt
     */
    public String getSalt() {
        return salt;
    }

    @Override
    public String toString() {
        return userId + ":" + hash + ":" + salt;
    }
}
