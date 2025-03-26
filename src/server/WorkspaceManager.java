package server;

import server.models.StatusCode;
import server.models.User;
import server.models.Workspace;
import server.utils.InputUtils;

import java.io.File;

public class WorkspaceManager {
    private static WorkspaceManager INSTANCE;
    private final FileStorageManager fsm;

    private WorkspaceManager() {
        fsm = FileStorageManager.getInstance();
    }

    /**
     * Get the instance of the WorkspaceManager
     *
     * @return The instance of the WorkspaceManager
     */
    public synchronized static WorkspaceManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WorkspaceManager();
        }

        return INSTANCE;
    }

    /**
     * Create a workspace for a user
     *
     * @param userId The ID of the user creating the workspace
     * @param workspace The name of the workspace
     * @return StatusCode.OK if the workspace was created, StatusCode.NOK if the workspace could not be created, StatusCode.BAD_REQUEST if the input is invalid
     */
    public StatusCode createWorkspace(String userId, String workspace) {
        if (workspace == null || workspace.isEmpty() || userId == null || userId.isEmpty()) {
            return StatusCode.BAD_REQUEST;
        }

        if (!InputUtils.isValidWorkspaceId(workspace) || !InputUtils.isValidUserId(userId)) {
            return StatusCode.NOK;
        }

        if (fsm.createWorkspace(userId, workspace)) {
            return StatusCode.OK;
        }

        return StatusCode.NOK;
    }

    public StatusCode addUserToWorkspace(String userId, String newUser, String workspaceId) {
        Workspace ws = fsm.getWorkspace(workspaceId);
        if (ws == null) {
            return StatusCode.NOWS;
        }

        if (!InputUtils.isValidUserId(newUser)) {
            return StatusCode.NOK;
        }

        AuthenticationManager authManager = AuthenticationManager.getInstance();
        if (authManager.getUser(newUser) == null) {
            return StatusCode.NOUSER;
        }

        if (ws.hasMember(newUser)) {
            return StatusCode.NOPERM;
        }

        if (ws.isOwner(userId)) {
            if (fsm.addUserToWorkspace(workspaceId, newUser)) {
                return StatusCode.OK;
            }
        } else {
            return StatusCode.NOPERM;
        }

        return StatusCode.NOPERM;
    }

    /**
     * Is the user in the workspace
     *
     * @param userId the user id
     * @param workspaceId the workspace id
     * @return true if the user is in the workspace, false otherwise
     */
    public boolean isUserInWorkspace(String userId, String workspaceId) {
        Workspace ws = fsm.getWorkspace(workspaceId);
        if (ws == null) {
            return false;
        }

        return ws.hasMember(userId);
    }

    public boolean uploadFile(String userId, String workspaceId, File file, String fileName) {
        Workspace ws = fsm.getWorkspace(workspaceId);
        if (ws == null) {
            return false;
        }

        if (ws.hasMember(userId)) {
            return fsm.uploadFile(workspaceId, file, fileName);
        }

        return false;
    }

    /**
     * List the workspaces for a user
     *
     * @param userId The ID of the user
     * @return An array of workspace IDs
     */
    public String[] listWorkspaces(String userId) {
        return fsm.listWorkspaceIds(userId);
    }

    /**
     * List the files in a workspace
     *
     * @param userId The ID of the user
     * @param workspaceId The ID of the workspace
     * @return An array of file names
     */
    public String[] listWorkspaceFiles(String userId, String workspaceId) {
        if (isUserInWorkspace(userId, workspaceId)) {
            return fsm.listWorkspaceFiles(workspaceId);
        }

        return new String[0];
    }

    /**
     * Is the file in the workspace
     *
     * @param fileName the name of the file
     * @param workspaceId the id of the workspace
     * @return true if the file is in the workspace, false otherwise
     */
    public boolean isFileInWorkspace(String fileName, String workspaceId) {
        return fsm.isFileInWorkspace(fileName, workspaceId);
    }

    /**
     * Get the file from the workspace
     *
     * @param fileName the name of the file
     * @param workspaceId the id of the workspace
     * @return the file if it exists, null otherwise
     */
    public File getFile(String fileName, String workspaceId) {
        return fsm.getFile(fileName, workspaceId);
    }

    /**
     * Removes a file from the workspace
     *
     * @param userId the id of the user
     * @param workspaceId the id of the workspace
     * @param fileName the name of the file
     * @return StatusCode.OK if the file was removed, StatusCode.NOK if the file could not be removed, StatusCode.NOWS
     * if the workspace does not exist, StatusCode.NOPERM if the user does not have permission to remove the file
     */
    public StatusCode removeFileFromWorkspace(String userId, String workspaceId, String fileName) {
        Workspace ws = fsm.getWorkspace(workspaceId);
        if (ws == null) {
            return StatusCode.NOWS;
        }

        if (ws.hasMember(userId)) {
            boolean success = fsm.deleteFile(fileName, workspaceId);
            if (success) {
                return StatusCode.OK;
            } else {
                return StatusCode.NOK;
            }
        } else {
            return StatusCode.NOPERM;
        }
    }
}