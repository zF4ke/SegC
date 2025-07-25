package server;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    private final ReadWriteLock metaLock = new ReentrantReadWriteLock();
    private final ConcurrentMap<String, ReadWriteLock> wsLocks = new ConcurrentHashMap<>();

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

    private ReadWriteLock getWorkspaceLock(String workspaceId) {
        return wsLocks.computeIfAbsent(workspaceId, k -> new ReentrantReadWriteLock());
    }

    /**
     * Gets a workspace from the file.
     *
     * @param workspaceId the workspace ID
     * @return the workspace, or null if the workspace does not exist
     */
    public Workspace getWorkspace(String workspaceId) {
        metaLock.readLock().lock();
        try {
            MySharingServer.verifyWorkspacesMac();

            try (Scanner scanner = new Scanner(new File(WORKSPACES_FILE_PATH))) {
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
        } finally {
            metaLock.readLock().unlock();
        }
    }

    /**
     * Create a new workspace.
     *
     * @param userId the user ID of the owner
     * @param name the name of the workspace
     * @return true if the workspace was created, false otherwise
     */
    public boolean createWorkspace(String userId, String name) {
        metaLock.writeLock().lock();
        try {
            MySharingServer.verifyWorkspacesMac();

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
                    System.out.println("[FILE STORAGE] Erro ao apagar workspace: " + ex.getMessage());
                }

                System.out.println("[FILE STORAGE] Erro ao criar workspace: " + e.getMessage());
                return false;
            }

            MySharingServer.updateWorkspacesMac();
            return true;
        } finally {
            metaLock.writeLock().unlock();
        }
    }



    /**
     * Saves a workspace key file (e.g. "workspace001.key.userId") into the workspace directory.
     *
     * @param workspaceId the identifier of the workspace (must match its folder under data/workspaces)
     * @param keyFileName the filename for the key (e.g. "<workspaceId>.key.<userId>")
     * @param data        the raw bytes to write (typically Base64-encoded salt:key)
     * @return true if the file was saved successfully, false otherwise
     */
    public boolean saveWorkspaceKey(String workspaceId, String keyFileName, byte[] data) {
        Path workspaceDir = Paths.get(WORKSPACES_DIR_PATH + workspaceId);
        Path keyFilePath = workspaceDir.resolve(keyFileName);

        // Check if the workspace directory exists
        if (!Files.exists(workspaceDir)) {
            System.err.println("[FILE STORAGE] Workspace não encontrado: " + workspaceId);
            return false;
        }

        // Check if the key file already exists
        if (Files.exists(keyFilePath)) {
            System.err.println("[FILE STORAGE] Arquivo de chave já existe: " + keyFileName);
            return false;
        }

        try {
            // Write the data to the key file
            Files.write(keyFilePath, data);
            return true;
        } catch (IOException e) {
            System.err.println("[FILE STORAGE] Erro ao salvar arquivo de chave: " + e.getMessage());
            return false;
        }
    }

    /**
     * Add a user to the workspace.
     *
     * @param workspaceId the workspace ID
     * @param userId the user ID to add to the workspace
     * @return true if the user was added, false otherwise
     */
    public boolean addUserToWorkspace(String workspaceId, String userId)  {
        ReadWriteLock lock = getWorkspaceLock(workspaceId);
        lock.writeLock().lock();
        try {
            MySharingServer.verifyWorkspacesMac();

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

                MySharingServer.updateWorkspacesMac();
                return true;
            } catch (IOException e) {
                System.err.println("[FILE STORAGE] Erro ao adicionar usuário ao workspace: " + e.getMessage());
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * List all workspaces that a user is a member of.
     *
     * @param usernameId the user ID
     * @return an array of workspace IDs
     */
    public String[] listWorkspaceIds(String usernameId) {
        metaLock.readLock().lock();

        try {
            MySharingServer.verifyWorkspacesMac();

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
        } finally {
            metaLock.readLock().unlock();
        }
    }

    /**
     * List all files in a workspace.
     *
     * @param workspaceId the workspace ID
     * @return an array of file names
     */
    public String[] listWorkspaceFiles(String workspaceId) {
        ReadWriteLock lock = getWorkspaceLock(workspaceId);
        lock.readLock().lock();

        try {
            MySharingServer.verifyWorkspacesMac();

            File workspaceDir = new File(WORKSPACES_DIR_PATH + workspaceId);
            return workspaceDir.list();
        } finally {
            lock.readLock().unlock();
        }
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
        ReadWriteLock lock = getWorkspaceLock(workspaceId);
        lock.writeLock().lock();

        try {
            MySharingServer.verifyWorkspacesMac();

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

                MySharingServer.updateWorkspacesMac();
                return true;
            } catch (IOException e) {
                System.err.println("[FILE STORAGE] Erro ao fazer upload do arquivo: " + e.getMessage());
                return false;
            }
        } finally {
            lock.writeLock().unlock();
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
        MySharingServer.verifyWorkspacesMac();

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
        ReadWriteLock lock = getWorkspaceLock(workspaceId);
        lock.readLock().lock();

        try {
            MySharingServer.verifyWorkspacesMac();

            if (fileName == null || workspaceId == null) {
                return null;
            }

            String dir = WORKSPACES_DIR_PATH + workspaceId;
            return new File(dir, fileName);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Delete a file from a workspace.
     *
     * @param fileName the name of the file
     * @param workspaceId the workspace ID
     * @return true if the file was deleted, false otherwise
     */
    public boolean deleteFile(String fileName, String workspaceId) {
        ReadWriteLock lock = getWorkspaceLock(workspaceId);
        lock.writeLock().lock();

        try {
            MySharingServer.verifyWorkspacesMac();

            File file = getFile(fileName, workspaceId);
            if (file == null) {
                return false;
            }

            File signatureFile = getSignatureFile(fileName, workspaceId);
            if (signatureFile != null) {
                signatureFile.delete();
            }

            MySharingServer.updateWorkspacesMac();
            return file.delete();
        } finally {
            lock.writeLock().unlock();
        }
    }


    //TODO
    //REDO THIS
    public boolean isSignatureFileInWorkspace(String fileName, String workspaceId) {
        if (fileName == null || workspaceId == null) {
            return false;
        }
        if (getSignatureFile(fileName, workspaceId) == null) {
            return false;
        } else {
            return true;
        }
    }

    public File getSignatureFile(String fileName, String workspaceId) {
        ReadWriteLock lock = getWorkspaceLock(workspaceId);
        lock.readLock().lock();

        try {
            MySharingServer.verifyWorkspacesMac();

            String signatureFileName = fileName + ".signed";

            File baseFile = getFile(signatureFileName, workspaceId);
            if (baseFile == null) {
                return null;
            }

            File directory = baseFile.getParentFile();
            if (directory == null || !directory.isDirectory()) {
                return null;
            }

            String baseName = baseFile.getName();

            File[] matches = directory.listFiles((dir, name) -> name.startsWith(baseName));
            if (matches != null && matches.length > 0) {
                return matches[0];
            }

            return null;
        } finally {
            lock.readLock().unlock();
        }
    }
}