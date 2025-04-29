package server;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;

import server.models.StatusCode;
import server.models.Workspace;
import server.utils.InputUtils;
import server.utils.ServerSecurityUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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
     * @param userId ID of the user creating the workspace
     * @param workspaceName Name of the workspace
     * @param workspacePassword Password for the workspace
     * @return StatusCode.OK or NOK
     */
    public StatusCode createWorkspace(String userId, String workspaceName, String workspacePassword) {
        // Validações iniciais
        if (workspaceName == null || workspaceName.isEmpty() || userId == null || userId.isEmpty()
                || workspacePassword == null || workspacePassword.isEmpty()) {
            return StatusCode.BAD_REQUEST;
        }
        if (!InputUtils.isValidWorkspaceId(workspaceName) || !InputUtils.isValidUserId(userId)) {
            return StatusCode.NOK;
        }

        try {
            // Criar diretório do workspace
            if (!fsm.createWorkspace(userId, workspaceName)) {
                return StatusCode.NOK;
            }

            // Gerar salt e derivar chave AES via PBKDF2WithHmacSHA256
            byte[] salt = ServerSecurityUtils.genSalt();
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(workspacePassword.toCharArray(), salt,
                    ServerSecurityUtils.DEFAULT_ITERATION_COUNT, 128);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey aesKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            // Cifrar a chave AES com RSA/OAEP com a public key do owner
            PublicKey ownerPub = ServerSecurityUtils.getUserPublicKeyFromTruststore(userId);
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, ownerPub);
            byte[] encryptedKey = rsaCipher.doFinal(aesKey.getEncoded());

            // Concatenar salt + encryptedKey em Base64
            String encodedSalt = Base64.getEncoder().encodeToString(salt);
            String encodedKey  = Base64.getEncoder().encodeToString(encryptedKey);
            String keyData     = encodedKey + ":" + encodedSalt;

            // Gravar o ficheiro .key.<userId> no workspace
            String workspaceId = userId + "_" + workspaceName;
            String keyFileName = workspaceId + ".key." + userId;
            boolean saved = fsm.saveWorkspaceKey(workspaceId, keyFileName, keyData.getBytes(StandardCharsets.UTF_8));
            if (!saved) {
                return StatusCode.NOK;
            }

            return StatusCode.OK;

        } catch (Exception e) {
            e.printStackTrace();
            return StatusCode.NOK;
        }
    }

    /**
     * Add a user to a workspace
     *
     * @param ownerId The ID of the user who owns the workspace
     * @param newUserId The ID of the user to be added
     * @param workspaceId The ID of the workspace
     * @return StatusCode.OK if the user was added, StatusCode.NOK if the user could not be added, StatusCode.NOWS if the workspace does not exist, StatusCode.NOPERM if the user does not have permission to add the user
     */
    public StatusCode addUserToWorkspace(String ownerId, String newUserId, String workspaceId) {
        Workspace ws = fsm.getWorkspace(workspaceId);
        if (ws == null) {
            return StatusCode.NOWS;
        }

        if (!InputUtils.isValidUserId(newUserId)) {
            return StatusCode.NOK;
        }

        AuthenticationManager authManager = AuthenticationManager.getInstance();
        if (authManager.getUser(newUserId) == null) {
            return StatusCode.NOUSER;
        }

        if (ws.hasMember(newUserId)) {
            return StatusCode.NOPERM;
        }

        if (!ws.isOwner(ownerId)) {
            return StatusCode.NOPERM;
        }

        boolean added = fsm.addUserToWorkspace(workspaceId, newUserId);
        if (!added) {
            return StatusCode.NOK;
        }

        return StatusCode.OK;
    }

    /**
     * Does the workspace exist
     *
     * @param workspaceId the id of the workspace
     * @return true if the workspace exists, false otherwise
     */
    public boolean workspaceExists(String workspaceId) {
        return fsm.getWorkspace(workspaceId) != null;
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

    public boolean  isSignatureFileInWorkspace(String signatureFileName, String workspaceId) {
        return fsm.isSignatureFileInWorkspace(signatureFileName, workspaceId);
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
                return StatusCode.NOT_FOUND;
            }
        } else {
            return StatusCode.NOPERM;
        }
    }
}