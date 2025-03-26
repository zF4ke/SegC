package server;

import server.models.StatusCode;
import server.models.User;
import server.models.Workspace;
import server.utils.InputUtils;

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

    public boolean[] uploadFile(User user, String workspaceId, String[] filePaths) {
        Workspace ws = fsm.getWorkspace(workspaceId);
        if (ws == null) {
            return new boolean[filePaths.length];
        }

        if (ws.hasMember(user.getUserId())) {
            return fsm.uploadFiles(workspaceId, filePaths);
        }

        return new boolean[filePaths.length]; //all false default ok
    }

//    public String[] downloadFile(String user, String workspace, String[] filePaths) {
//        if (fsm.isUserInWorkspace(user, workspace)) {
//            return fsm.downloadFiles(workspace, filePaths);
//        }
//
//        return new String[filePaths.length]; //all null default ok
//    }
//
//    public boolean[] removeFiles(String user, String workspace, String[] filePaths) {
//        if (fsm.isUserInWorkspace(user, workspace)) {
//            return fsm.removeFiles(workspace, filePaths);
//        }
//        return new boolean[filePaths.length];
//    }
//
    //list workspaces´
    public String[] listWorkspaces(String userId) {
        return fsm.listWorkspaceIds(userId);
    }

    //list files in workspace
    public String[] listWorkspaceFiles(String userId, String workspaceId) {
        if (isUserInWorkspace(userId, workspaceId)) {
            return fsm.listWorkspaceFiles(workspaceId);
        }

        return new String[0];
    }
}