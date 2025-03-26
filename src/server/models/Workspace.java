package server.models;

import java.util.List;

/**
 * Represents a workspace.
 */
public class Workspace {
    private final String id;
    private final String ownerUsername;
    private final List<String> members;

    /**
     * Create a new workspace.
     *
     * @param id the id of the workspace
     * @param ownerUsername the username of the owner
     * @param members the list of members
     */
    public Workspace(String id, String ownerUsername, List<String> members) {
        this.id = id;
        this.ownerUsername = ownerUsername;
        this.members = members;
    }

    /**
     * Get the name of the workspace.
     *
     * @return the name
     */
    public String getId() {
        return id;
    }

    /**
     * Get the username of the owner.
     *
     * @return the username
     */
    public String getOwnerUsername() {
        return ownerUsername;
    }

    /**
     * Get the list of members.
     *
     * @return the list of members
     */
    public List<String> getMembers() {
        return members;
    }

    /**
     * Check if a user is a member of the workspace.
     *
     * @param username the username of the user
     * @return true if the user is a member, false otherwise
     */
    public boolean hasMember(String username) {
        return members.contains(username);
    }

    /**
     * Check if a user is the owner of the workspace.
     *
     * @param username the username of the user
     * @return true if the user is the owner, false otherwise
     */
    public boolean isOwner(String username) {
        return ownerUsername.equals(username);
    }
}
