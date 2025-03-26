package server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import server.models.Workspace;

/*
* workspaces.txt example:
* id:owner_username:user1,user2,user3,user4
* workspace001:owner_username:user1,user2,user3,user4
*
*
*/

public class FileStorageManager {
    private static FileStorageManager INSTANCE;
    private static final String DATA_DIR_PATH = "data/";
    private static final String WORKSPACES_FILE_PATH = "data/workspaces.txt";
    private static final String WORKSPACES_DIR_PATH = "data/workspaces/";

    /**
     * Create a new file storage manager.
     */
    private FileStorageManager() {
        try {
            // Create directory paths if they don't exist
            Path dataDir = Paths.get(DATA_DIR_PATH);
            Path workspacesDir = Paths.get(WORKSPACES_DIR_PATH);
            Path workspacesFile = Paths.get(WORKSPACES_FILE_PATH);

            Files.createDirectories(dataDir);
            Files.createDirectories(workspacesDir);

            // Create workspaces file only if it doesn't exist
            if (!Files.exists(workspacesFile)) {
                Files.createFile(workspacesFile);
            }
        } catch (IOException e){
            System.out.println("[FILE STORAGE] Erro ao criar diretórios e arquivos: " + e.getMessage());
        }

    }

    /**
     * Get the instance of the file storage manager.
     *
     * @return the instance
     */
    public synchronized static FileStorageManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FileStorageManager();
        }
        
        return INSTANCE;
    }

    /**
     * Gets a workspace from the file.
     *
     * @param workspaceId the workspace ID
     * @return the workspace, or null if the workspace does not exist
     */
    public Workspace getWorkspace(String workspaceId) {
        try (Scanner scanner = new Scanner(new File(WORKSPACES_FILE_PATH))){
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(":");

                int NUM_PARTS = 3;
                if (parts.length != NUM_PARTS) {
                    System.out.println("[FILE STORAGE] Erro ao ler workspace: Formato inválido");
                    continue;
                }

                String id = parts[0];
                String ownerUsername = parts[1];
                List<String> members = Arrays.asList(parts[2].split(","));

                if (id.equals(workspaceId)) {
                    return new Workspace(id, ownerUsername, members);
                }
            }

        } catch (IOException e) {
            System.err.println("[FILE STORAGE] Erro ao carregar workspaces: " + e.getMessage());
        }

        return null;
    }

    /**
     * Create a new workspace.
     *
     * @param userId the user ID of the owner
     * @param name the name of the workspace
     * @return true if the workspace was created, false otherwise
     */
    public boolean createWorkspace(String userId, String name) {
        String workspaceId = userId + "_" + name;

        if (this.getWorkspace(workspaceId) != null) {
            System.err.println("[FILE STORAGE] Workspace já existe: " + workspaceId);
            return false;
        }

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(WORKSPACES_FILE_PATH, true))) {
            //creating the dir for the workspace
            Files.createDirectory(Paths.get(WORKSPACES_DIR_PATH + workspaceId));

            //adding the workspace to the workspace.txt file
            String newLine = workspaceId + ":" + userId + ":" + userId;
            bufferedWriter.write(newLine);
            bufferedWriter.newLine();
        } catch (IOException e) {
            // Remove dir if it was created
            try {
                Files.delete(Paths.get(WORKSPACES_DIR_PATH + workspaceId));
            } catch (IOException ex) {
                System.out.println("[FILE STORAGE] Erro ao deletar workspace: " + ex.getMessage());
            }

            System.out.println("[FILE STORAGE] Erro ao criar workspace: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Add a user to the workspace.
     *
     * @param workspaceId the workspace ID
     * @param userId the user ID to add to the workspace
     * @return true if the user was added, false otherwise
     */
    public boolean addUserToWorkspace(String workspaceId, String userId)  {
        try {
            File file = new File(WORKSPACES_FILE_PATH);
            Scanner scanner = new Scanner(file);
            StringBuilder newContent = new StringBuilder();
            boolean workspaceFound = false;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(":");

                int NUM_PARTS = 3;
                if (parts.length != NUM_PARTS) {
                    System.out.println("[FILE STORAGE] Erro ao ler workspace: Formato inválido");
                    continue;
                }

                String id = parts[0];
                String ownerUsername = parts[1];
                List<String> members = new ArrayList<>(Arrays.asList(parts[2].split(",")));

                if (id.equals(workspaceId)) {
                    workspaceFound = true;

                    if (members.contains(userId)) {
                        System.err.println("[FILE STORAGE] Usuário já é membro do workspace: " + userId);
                        return false;
                    }

                    members.add(userId);
                    newContent
                            .append(id)
                            .append(":")
                            .append(ownerUsername)
                            .append(":");
                    for (String member : members) {
                        newContent.append(member).append(",");
                    }
                    newContent.deleteCharAt(newContent.length() - 1);
                } else {
                    newContent.append(line);
                }

                newContent.append("\n");
            }

            if (!workspaceFound) {
                System.err.println("[FILE STORAGE] Workspace não encontrado: " + workspaceId);
                return false;
            }

            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(newContent.toString());
            fileWriter.close();

            return true;
        } catch (IOException e) {
            System.err.println("[FILE STORAGE] Erro ao adicionar usuário ao workspace: " + e.getMessage());
            return false;
        }
    }

    /**
     * List all workspaces that a user is a member of.
     *
     * @param usernameId the user ID
     * @return an array of workspace IDs
     */
    public String[] listWorkspaceIds(String usernameId) {
        try (Scanner scanner = new Scanner(new File(WORKSPACES_FILE_PATH))) {
            List<String> workspaceIds = new ArrayList<>();

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(":");

                int NUM_PARTS = 3;
                if (parts.length != NUM_PARTS) {
                    System.out.println("[FILE STORAGE] Erro ao ler workspace: Formato inválido");
                    continue;
                }

                String id = parts[0];
                String ownerUsername = parts[1];
                List<String> members = Arrays.asList(parts[2].split(","));

                if (ownerUsername.equals(usernameId) || members.contains(usernameId)) {
                    workspaceIds.add(id);
                }
            }

            return workspaceIds.toArray(new String[0]);
        } catch (IOException e) {
            System.err.println("[FILE STORAGE] Erro ao listar workspaces: " + e.getMessage());

            return new String[0];
        }
    }

    /**
     * List all files in a workspace.
     *
     * @param workspaceId the workspace ID
     * @return an array of file names
     */
    public String[] listWorkspaceFiles(String workspaceId) {
        File workspaceDir = new File(WORKSPACES_DIR_PATH + workspaceId);
        return workspaceDir.list();
    }

    /**
     * Upload a file to a workspace.
     *
     * @param workspaceId the workspace ID
     * @param file the file to upload
     * @param fileName the name of the file
     * @return true if the file was uploaded, false otherwise
     */
    public boolean uploadFile(String workspaceId, File file, String fileName) {
        try {
            // workspace path
            Path workspacePath = Paths.get(WORKSPACES_DIR_PATH + workspaceId);
            if (!Files.exists(workspacePath)) {
                System.err.println("[FILE STORAGE] Workspace não encontrado: " + workspaceId);
                return false;
            }

            // file path
            Path filePath = Paths.get(WORKSPACES_DIR_PATH + workspaceId + "/" + fileName);

            // move file to workspace
            Files.move(file.toPath(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return true;
        } catch (IOException e) {
            System.err.println("[FILE STORAGE] Erro ao fazer upload do arquivo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a file is in a workspace.
     *
     * @param fileName the name of the file
     * @param workspaceId the workspace ID
     * @return true if the file is in the workspace, false otherwise
     */
    public boolean isFileInWorkspace(String fileName, String workspaceId) {
        File file = getFile(fileName, workspaceId);
        if (file == null) {
            return false;
        }
        return file.exists();
    }

    /**
     * Get a file from a workspace.
     *
     * @param fileName the name of the file
     * @param workspaceId the workspace ID
     * @return the file if it exists, null otherwise
     */
    public File getFile(String fileName, String workspaceId) {
    if (fileName == null || workspaceId == null) {
        return null;
    }
    String dir = WORKSPACES_DIR_PATH + workspaceId;
    return new File(dir, fileName);
}

    /**
     * Delete a file from a workspace.
     *
     * @param fileName the name of the file
     * @param workspaceId the workspace ID
     * @return true if the file was deleted, false otherwise
     */
    public boolean deleteFile(String fileName, String workspaceId) {
        File file = getFile(fileName, workspaceId);
        System.out.println("[FILE STORAGE] Delete do arquivo: " + fileName);
        System.out.println("workspaceid: " + workspaceId);
        System.out.println("file:" + file.getPath());
        if (file == null) {
            System.out.println("teste1 fudeu");
            return false;
        }
        return file.delete();
    }

}