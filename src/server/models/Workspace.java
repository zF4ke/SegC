package server.models;

import java.util.List;

/**
 * Represents a workspace.
 */
public class Workspace {
    private final String name;
    private final String ownerUsername;
    private final List<String> members;

    /**
     * Create a new workspace.
     *
     * @param name the name of the workspace
     * @param ownerUsername the username of the owner
     * @param members the list of members
     */
    public Workspace(String name, String ownerUsername, List<String> members) {
        this.name = name;
        this.ownerUsername = ownerUsername;
        this.members = members;
    }

    /**
     * Get the name of the workspace.
     *
     * @return the name
     */
    public String getName() {
        return name;
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
     * Add a member to the workspace.
     *
     * @param username the username of the member
     */
    public void addMember(String username) {
        members.add(username);
    }

    /**
     * Remove a member from the workspace.
     *
     * @param username the username of the member
     */
    public void removeMember(String username) {
        members.remove(username);
    }

    /**
     * Check if a user is a member of the workspace.
     *
     * @param username the username of the user
     * @return true if the user is a member, false otherwise
     */
    public boolean isMember(String username) {
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
