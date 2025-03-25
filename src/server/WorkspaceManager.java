package server;

public class WorkspaceManager {
    
    FileStorageManager fsm; 
    public WorkspaceManager() {
        fsm = FileStorageManager.getInstance();
    }

    public boolean createWorkspace(String username, String workspace) {
        return fsm.createWorkspace(username, workspace);
    }

    public boolean addUserToWorkspace(String owner, String user, String workspace) {
        if (fsm.isUserOwner(workspace, owner)) {
            return fsm.addUserToWorkspace(user, workspace);
        }
        return  false;
    }

    public boolean[] uploadFile(String user, String workspace, String[] filePaths) {
        if (fsm.isUserInWorkspace(user, workspace)) {
            return fsm.uploadFiles(workspace, filePaths);
        }

        return new boolean[filePaths.length]; //all false default ok
    }

    public String[] downloadFile(String user, String workspace, String[] filePaths) {
        if (fsm.isUserInWorkspace(user, workspace)) {
            return fsm.downloadFiles(workspace, filePaths);
        }

        return new String[filePaths.length]; //all null default ok
    }

    public boolean[] removeFiles(String user, String workspace, String[] filePaths) {
        if (fsm.isUserInWorkspace(user, workspace)) {
            return fsm.removeFiles(workspace, filePaths);
        }
        return new boolean[filePaths.length];
    }

    //list workspaces´
    public String[] listWorkspaces(String user) {
        return fsm.listWorkspaces(user);
    }

    //list files in workspace
    public String[] listFiles(String user, String workspace) {
        if (fsm.isUserInWorkspace(user, workspace)) {
            return fsm.listFiles(workspace);
        }
        return new String[0];
    }

}