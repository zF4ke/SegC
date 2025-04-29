package client;

import server.models.*;
import server.utils.NetworkUtils;
//import shared.FileSecurityUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;


public class NetworkManager {
    private final DataInputStream in;
    private final DataOutputStream out;

    /**
     * Create a new network manager.
     *
     * @param in the input stream
     * @param out the output stream
     */
    public NetworkManager(DataInputStream in, DataOutputStream out) {
        this.in = in;
        this.out = out;
    }

    /**
     * Sends a request to the server to create a workspace.
     *
     * @param workspaceName the workspace name
     */
    public void createWorkspace(String workspaceName, String workspacePassword) {
        BodyJSON body = new BodyJSON();
        body.put("workspaceName", workspaceName);
        body.put("workspacePassword", workspacePassword);

        Response response = sendRequest(body, "createworkspace");
        if (response != null) {
            try {
                BodyJSON responseBody = response.getBodyJSON();
                String message = responseBody.get("message");
                if (message == null) message = "";

                System.out.println("Resposta: " + response.getStatus() + " # " + message);
            } catch (Exception e) {
                System.err.println("[CLIENT] Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

    /**
     * Sends a request to the server to add a user to a workspace.
     *
     * @param user the user
     * @param workspaceId the workspace ID
     */
    public void addUserToWorkspace(String user, String workspaceId, String ownerId) {
        // get key from the server
        BodyJSON bodyKey = new BodyJSON();
        bodyKey.put("action", "init");
        bodyKey.put("workspaceId", workspaceId);
        Response responseKey = sendRequest(bodyKey, "downloadkeyfromworkspace");
        String fileId = null;
        if (responseKey != null) {
            try {
                StatusCode status = responseKey.getStatus();
                if (status != StatusCode.OK) {
                    System.out.println("Resposta: " + responseKey.getStatus());
                    return;
                }

                String fileName = workspaceId + ".key." + ownerId;
                StatusCode statusKey = receiveKeyFromServer(fileName, workspaceId, in, out);
                if (statusKey != StatusCode.OK) {
                    System.out.println("Resposta: " + statusKey);
                    return;
                }

                File file = new File(fileName);
//                When im adding a user to the workspace, i need to download the workspace key, decrypt it with the owner private key (on the client) and then encrypt it with the new user's public key. Then i send the key back to the server and it saves it as <ws>.key.<newuserid>
//                Just like in the Upload/Download file To/From workspace handlers, I need actions and routes to do that, so please ajust the AddUserToWorkspaceHandler to do all that.

                // decrypt the file with the owner private key
                // encrypt the file with the new user public key
                // save the file as <ws>.key.<newuserid>
                //System.out.println("Resposta: " + statusKey);
                PrivateKey ownerPrivateKey = ClientSecurityUtils.getUserPrivateKeyFromKeyStore(ownerId);
                String keyData = Files.readString(file.toPath());
                String[] keyParts = keyData.split(":");
                byte[] wrappedAesKey = Base64.getDecoder().decode(keyParts[0]);
                byte[] salt = Base64.getDecoder().decode(keyParts[1]);

                // decrypt the key with the owner private key
                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
                rsaCipher.init(Cipher.DECRYPT_MODE, ownerPrivateKey);

                byte[] aesKeyBytes = rsaCipher.doFinal(wrappedAesKey);

                SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

                // encrypt the key with the new user public key
                // get the new user public key
                PublicKey newUserPublicKey = ClientSecurityUtils.getUserPublicKeyFromKeyStore(user);

                // encrypt the key with the new user public key
                Cipher rsaCipher2 = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
                rsaCipher2.init(Cipher.ENCRYPT_MODE, newUserPublicKey);
                byte[] wrappedAesKey2 = rsaCipher2.doFinal(aesKey.getEncoded());

                // create the new key file
                String newKeyData = Base64.getEncoder().encodeToString(wrappedAesKey2) + ":" + Base64.getEncoder().encodeToString(salt);

                // save the new key file
                File newKeyFile = new File(workspaceId + ".key." + user);
                try (FileOutputStream fos = new FileOutputStream(newKeyFile)) {
                    fos.write(newKeyData.getBytes());
                }

                // send key to the server
                fileId = sendKeyToServer(newKeyFile.getPath(), workspaceId, in, out);

                if (fileId == null) {
                    System.out.println("Resposta: Erro ao adicionar o utilizador ao workspace");
                    return;
                }

                BodyJSON body = new BodyJSON();
                body.put("user", user);
                body.put("workspaceId", workspaceId);
                body.put("keyFileId", fileId);

                Response response = sendRequest(body, "addusertoworkspace");
                if (response != null) {
                    try {
                        BodyJSON responseBody = response.getBodyJSON();
                        String message = responseBody.get("message");
                        if (message == null) message = "";

                        System.out.println("Resposta: " + response.getStatus() + " # " + message);
                    } catch (Exception e) {
                        System.err.println("[CLIENT] Erro ao processar resposta: " + e.getMessage());
                    }
                }

                // delete the file
                Files.deleteIfExists(Paths.get(file.getPath()));

                // delete the new key file
                Files.deleteIfExists(Paths.get(newKeyFile.getPath()));
            } catch (Exception e) {
                System.err.println("[CLIENT] Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

    /**
     * Sends a request to the server to upload files to a workspace.
     *
     * @param userId the user ID
     * @param workspaceId the workspace ID
     * @param files the files
     */
    public void uploadFilesToWorkspace(String userId, String workspaceId, String[] files) {
        // 1. check if the user has permission to upload files to the workspace
        BodyJSON verifyBody = new BodyJSON();
        verifyBody.put("action", "verify");
        verifyBody.put("workspaceId", workspaceId);

        Response verifyResponse = sendRequest(verifyBody, "uploadfiletoworkspace");
        if (verifyResponse != null) {
            if (verifyResponse.getStatus() != StatusCode.OK) {
                System.out.println("Resposta: " + verifyResponse.getStatus());
                return;
            }
        }

        // 2. send files to the server
        boolean first = true;
        for (String file : files) {
            try {

                PrivateKey privateKey = ClientSecurityUtils.getUserPrivateKeyFromKeyStore(userId);
                File signatureFile = ClientSecurityUtils.createSignedFile(file,userId ,privateKey);
                StatusCode fileStatus = sendFileToServerWithSignature(file,signatureFile.getPath(), workspaceId, in, out);
                if (!signatureFile.delete()) {
                    System.err.println("[CLIENT] Erro ao apagar o ficheiro de assinatura: " + signatureFile.getPath());
                }
                //new code starts 
                
                //new code endds

                //System.out.print("\t" + file + ": " + status);
                if (!first) {
                    System.out.println("\t  " + file + ": " + fileStatus);
                } else {
                    first = false;
                    System.out.println("Resposta: " + file + ": " + fileStatus);
                }
            } catch (IOException e) {
                System.err.println("[CLIENT] Erro ao enviar ficheiro: " + e.getMessage());
            }
        }
    }

    /**
     * Sends a request to the server to download files from a workspace.
     *
     * @param workspaceId the workspace id
     * @param files the files
     */
    public void downloadFilesFromWorkspace(String user,String workspaceId, String[] files) {
        BodyJSON body = new BodyJSON();
        body.put("workspaceId", workspaceId);
        body.put("action", "verify");

        Response response = sendRequest(body, "downloadfilefromworkspace");
        if (response != null) {
            StatusCode status = response.getStatus();
            if (status != StatusCode.OK) {
                System.out.println("Resposta: " + response.getStatus());
                return;
            }
        }

        boolean first = true;
        for (String file : files) {
            try {
                StatusCode status = receiveFileFromServerWithSignature(user,file, workspaceId, in, out);
                if (!first) {
                    System.out.println("\t  " + file + ": " + status);
                } else {
                    first = false;
                    System.out.println("Resposta: " + file + ": " + status);
                }

            } catch (IOException e) {
                System.err.println("[CLIENT] Erro ao enviar ficheiro: " + e.getMessage());
            }
        }
    }

    /**
     * Sends a request to the server to remove files from a workspace.
     *
     * @param workspaceId the workspace id
     * @param files the files
     */
    public void removeFilesFromWorkspace(String workspaceId, String[] files) {
        BodyJSON bodyVerify = new BodyJSON();
        bodyVerify.put("workspaceId", workspaceId);

        // Verify if the user has permission to remove files from the workspace, using the listworkspacefiles route
        Response responseVerify = sendRequest(bodyVerify, "listworkspacefiles");
        if (responseVerify != null) {
            StatusCode status = responseVerify.getStatus();
            if (status != StatusCode.OK) {
                System.out.println("Resposta: " + responseVerify.getStatus());
                return;
            }
        }

        boolean first = true;
        for (String file : files) {
            BodyJSON body = new BodyJSON();
            body.put("workspaceId", workspaceId);
            body.put("fileName", file);

            Response response = sendRequest(body, "removefilefromworkspace");
            if (response != null) {
                try {
                    //System.out.println("Resposta: (" + response.getStatus() + ") " + message);
                    if (!first) {
                        System.out.println("\t  " + file + ": " + response.getStatus());
                    } else {
                        first = false;
                        System.out.println("Resposta: " + file + ": " + response.getStatus());
                    }

                } catch (Exception e) {
                    System.err.println("[CLIENT] Erro ao processar resposta: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Sends a request to the server to list workspaces.
     */
    public void listWorkspaces() {
        Response response = sendRequest(new BodyJSON(), "listworkspaces");
        if (response != null) {
            try {
                BodyJSON responseBody = response.getBodyJSON();
                String workspaceIds = responseBody.get("workspaceIds");
                if (workspaceIds == null) workspaceIds = "";

                System.out.println("Resposta: " + workspaceIds);
            } catch (Exception e) {
                System.err.println("[CLIENT] Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

    /**
     * Sends a request to the server to list the files in a workspace.
     *
     * @param workspaceId the workspace ID
     */
    public void listFilesWorkspace(String workspaceId) {
        BodyJSON body = new BodyJSON();
        body.put("workspaceId", workspaceId);

        Response response = sendRequest(body, "listworkspacefiles");
        if (response != null) {
            try {
                BodyJSON responseBody = response.getBodyJSON();
                if (response.getStatus() != StatusCode.OK) {
                    System.out.println("Resposta: " + response.getStatus());
                    return;
                }

                String files = responseBody.get("files");
                if (files == null) files = "";

                System.out.println("Resposta: " + files);
            } catch (Exception e) {
                System.err.println("[CLIENT] Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

    /**
     * Sends a request to the server and returns the response.
     *
     * @param body the request body
     * @param route the route
     *
     * @return the response
     */
    private Response sendRequest(BodyJSON body, String route) {
        try {
            Request request = new Request(
                    NetworkUtils.randomUUID(),
                    BodyFormat.JSON,
                    route,
                    body
            );

            out.write(request.toByteArray());

            return Response.fromStream(in);
        } catch (IOException e) {
            System.err.println("[CLIENT] Erro ao processar pedido: " + e.getMessage());
        }

        return null;
    }

    /**
     * Receives a file from the server.
     *
     * @param fileName the file name
     * @param workspaceId the workspace ID
     * @param in the input stream
     * @param out the output stream
     */
    private static StatusCode receiveFileFromServerWithSignature(String user,String fileName, String workspaceId, DataInputStream in, DataOutputStream out) throws IOException {
        // Step 1: Initialize the download
        BodyJSON initBody = new BodyJSON();
        initBody.put("action", "init");
        initBody.put("fileName", fileName);
        initBody.put("workspaceId", workspaceId);

        Request initRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "downloadfilefromworkspace",
                initBody
        );

        out.write(initRequest.toByteArray());
        Response initResponse = Response.fromStream(in);
        //System.out.println("[CLIENT] Resposta de inicialização: " + initResponse);

        if (initResponse.getStatus() != StatusCode.OK) {
            //System.err.println("[CLIENT] Erro ao inicializar download");
            return StatusCode.NOT_FOUND;
        }

        BodyJSON initResponseBody = initResponse.getBodyJSON();
        int totalChunks = Integer.parseInt(initResponseBody.get("chunks"));
        int fileSize = Integer.parseInt(initResponseBody.get("size"));
        String fileId = initResponseBody.get("fileId");

        // Step 2: Receive file chunks
        try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
            for (int chunkId = 0; chunkId < totalChunks; chunkId++) {
                BodyJSON chunkBody = new BodyJSON();
                chunkBody.put("action", "chunk");
                chunkBody.put("chunkId", String.valueOf(chunkId));
                chunkBody.put("fileId", fileId);

                Request chunkRequest = new Request(
                        NetworkUtils.randomUUID(),
                        BodyFormat.JSON,
                        "downloadfilefromworkspace",
                        chunkBody
                );

                out.write(chunkRequest.toByteArray());
                Response chunkResponse = Response.fromStream(in);
                if (!String.valueOf(chunkId).equals(chunkResponse.getHeader("CHUNK-ID"))) {
                    System.err.println("[CLIENT] Erro ao receber chunk " + chunkId);
                    Files.deleteIfExists(Paths.get(fileName));
                    return chunkResponse.getStatus();
                }

                if (!fileId.equals(chunkResponse.getHeader("FILE-ID"))) {
                    System.err.println("[CLIENT] Erro ao receber chunk " + chunkId);
                    Files.deleteIfExists(Paths.get(fileName));
                    return chunkResponse.getStatus();
                }

                //System.out.println("[CLIENT] Resposta de chunk " + chunkId + ": " + chunkResponse);

                if (chunkResponse.getStatus() != StatusCode.OK) {
                    System.err.println("[CLIENT] Erro ao receber chunk " + chunkId);
                    Files.deleteIfExists(Paths.get(fileName));
                    return chunkResponse.getStatus();
                }

                BodyRaw chunkData = chunkResponse.getBodyRaw();
                fileOut.write(chunkData.toBytes());
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Erro ao receber ficheiro: " + e.getMessage());
            Files.deleteIfExists(Paths.get(fileName));
            return StatusCode.NOK;
        }

        // Step 3: Complete the download
        BodyJSON completeBody = new BodyJSON();
        completeBody.put("action", "complete");
        completeBody.put("fileId", fileId);

        Request completeRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "downloadfilefromworkspace",
                completeBody
        );

        out.write(completeRequest.toByteArray());

        Response completeResponse = Response.fromStream(in);
        if (completeResponse.getStatus() != StatusCode.OK) {
            System.err.println("[CLIENT] Erro ao finalizar download!");
            Files.deleteIfExists(Paths.get(fileName));

            return completeResponse.getStatus();
        }

        
        // Step 4: Init the signature file
        initBody = new BodyJSON();
        initBody.put("action", "init_signature");
        initBody.put("fileName", fileName);
        initBody.put("workspaceId", workspaceId);

        initRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "downloadfilefromworkspace",
                initBody
        );

        out.write(initRequest.toByteArray());
        initResponse = Response.fromStream(in);
        //System.out.println("[CLIENT] Resposta de inicialização: " + initResponse);

        if (initResponse.getStatus() != StatusCode.OK) {
            //System.err.println("[CLIENT] Erro ao inicializar download");
            return StatusCode.NOT_FOUND;
        }

        initResponseBody = initResponse.getBodyJSON();
        totalChunks = Integer.parseInt(initResponseBody.get("chunks"));
        fileSize = Integer.parseInt(initResponseBody.get("size"));
        String signatureFileName = initResponseBody.get("fileName");
        fileId = initResponseBody.get("fileId");


        // Step 5 : Receive signature file chunks
        try (FileOutputStream fileOut2 = new FileOutputStream(signatureFileName)) {
            for (int chunkId = 0; chunkId < totalChunks; chunkId++) {
                BodyJSON chunkBody = new BodyJSON();
                chunkBody.put("action", "signature_chunk");
                chunkBody.put("chunkId", String.valueOf(chunkId));
                chunkBody.put("fileId", fileId);

                Request chunkRequest = new Request(
                        NetworkUtils.randomUUID(),
                        BodyFormat.JSON,
                        "downloadfilefromworkspace",
                        chunkBody
                );

                out.write(chunkRequest.toByteArray());
                Response chunkResponse = Response.fromStream(in);
                if (!String.valueOf(chunkId).equals(chunkResponse.getHeader("CHUNK-ID"))) {
                    System.err.println("[CLIENT] Erro ao receber chunk " + chunkId);
                    Files.deleteIfExists(Paths.get(signatureFileName));
                    return chunkResponse.getStatus();
                }

                if (!fileId.equals(chunkResponse.getHeader("FILE-ID"))) {
                    System.err.println("[CLIENT] Erro ao receber chunk " + chunkId);
                    Files.deleteIfExists(Paths.get(signatureFileName));
                    return chunkResponse.getStatus();
                }

                //System.out.println("[CLIENT] Resposta de chunk " + chunkId + ": " + chunkResponse);

                if (chunkResponse.getStatus() != StatusCode.OK) {
                    System.err.println("[CLIENT] Erro ao receber chunk " + chunkId);
                    Files.deleteIfExists(Paths.get(signatureFileName));
                    return chunkResponse.getStatus();
                }

                BodyRaw chunkData = chunkResponse.getBodyRaw();
                fileOut2.write(chunkData.toBytes());
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Erro ao receber ficheiro: " + e.getMessage());
            Files.deleteIfExists(Paths.get(fileName));
            Files.deleteIfExists(Paths.get(signatureFileName));
            return StatusCode.NOK;
        }


        //Step 6: Receive signature file completion

        completeBody = new BodyJSON();
        completeBody.put("action", "complete");
        completeBody.put("fileId", fileId);

        completeRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "downloadfilefromworkspace",
                completeBody
        );

        out.write(completeRequest.toByteArray());

        completeResponse = Response.fromStream(in);
        if (completeResponse.getStatus() != StatusCode.OK) {
            System.err.println("[CLIENT] Erro ao finalizar download!");
            Files.deleteIfExists(Paths.get(fileName));
            Files.deleteIfExists(Paths.get(signatureFileName));

            return completeResponse.getStatus();
        }


        if (completeResponse.getStatus() != StatusCode.OK) {
            System.err.println("[CLIENT] Erro ao finalizar download!");
            Files.deleteIfExists(Paths.get(fileName));
            Files.deleteIfExists(Paths.get(signatureFileName));

            return completeResponse.getStatus();
        }


        String userId = signatureFileName.split("\\.")[2];
        TrustStoreManager trustStoreManager = new TrustStoreManager(user);
        PublicKey publicKey = ClientSecurityUtils.getUserPublicKeyFromKeyStore(userId);


        if (ClientSecurityUtils.verifySignedFile(fileName, signatureFileName, publicKey)) {
            System.out.println("[CLIENT] Ficheiro recebido com sucesso e verificado!");
        } else {
            System.err.println("[CLIENT] Ficheiro recebido mas não verificado!");
            Files.deleteIfExists(Paths.get(fileName));
            Files.deleteIfExists(Paths.get(signatureFileName));
            return StatusCode.NOK;

        }

        

        //System.out.println("[CLIENT] Ficheiro recebido com sucesso!");
        return completeResponse.getStatus();
    }

    /**
     * Sends a file to the server.
     *
     * @param filePath the file path
     * @param in the input stream
     * @param out the output stream
     */
    private static StatusCode sendFileToServerWithSignature(String filePath,String signatureFilePath, String workspaceId, DataInputStream in, DataOutputStream out) throws IOException {
        File file = new File(filePath);
        File signatureFile = new File(signatureFilePath);
        if (!file.exists()) {
            System.err.println("[CLIENT] Ficheiro não encontrado: " + filePath);
            return StatusCode.NOT_FOUND;
        }
        if (!signatureFile.exists()) {
            System.err.println("[CLIENT] Assinatura não encontrado: " + filePath);
            return StatusCode.NOT_FOUND;
        }

        // Step 1: Initialize the upload
        //System.out.println("[CLIENT] Iniciando envio do ficheiro: " + file.getName());

        BodyJSON initBody = new BodyJSON();
        initBody.put("action", "init");
        initBody.put("workspaceId", workspaceId);
        initBody.put("fileName", file.getName());
        initBody.put("size", String.valueOf(file.length()));

        int chunkSize = 1024 * 64; // 64KB chunks
        int totalChunks = (int) Math.ceil((double) file.length() / chunkSize);
        initBody.put("chunks", String.valueOf(totalChunks));

        Request initRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "uploadfiletoworkspace",
                initBody
        );

        out.write(initRequest.toByteArray());
        Response initResponse = Response.fromStream(in);
        //System.out.println("[CLIENT] Resposta de inicialização: " + initResponse);

        if (initResponse.getStatus() != StatusCode.OK) {
            System.err.println("[CLIENT] Erro ao inicializar upload");
            return initResponse.getStatus();
        }

        BodyJSON initResponseBody = initResponse.getBodyJSON();
        String fileId = initResponseBody.get("fileId");

        // Step 2: Send file chunks
        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[chunkSize];
            int chunkId = 0;
            int bytesRead;

            while ((bytesRead = fileIn.read(buffer)) > 0) {
                byte[] chunkData;
                if (bytesRead < buffer.length) {
                    chunkData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                } else {
                    chunkData = buffer;
                }

                BodyRaw chunkBody = new BodyRaw(chunkData);
                Request chunkRequest = new Request(
                        NetworkUtils.randomUUID(),
                        BodyFormat.RAW,
                        "uploadfiletoworkspace",
                        chunkBody
                );
                chunkRequest.addHeader("FILE-ID", fileId);
                chunkRequest.addHeader("CHUNK-ID", String.valueOf(chunkId));
                chunkRequest.addHeader("TYPE", "CHUNK");

                //System.out.println("[CLIENT] Enviando chunk " + (chunkId + 1) + "/" + (totalChunks));
                out.write(chunkRequest.toByteArray());

                Response chunkResponse = Response.fromStream(in);
                if (chunkResponse.getStatus() != StatusCode.OK) {
                    System.err.println("[CLIENT] Erro ao enviar chunk " + chunkId);
                    return chunkResponse.getStatus();
                }

                chunkId++;
            }
        }

        // Step 3: Complete the file upload
        BodyJSON completeBody = new BodyJSON();
        completeBody.put("action", "complete");
        completeBody.put("fileId", fileId);

        Request completeRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "uploadfiletoworkspace",
                completeBody
        );

        out.write(completeRequest.toByteArray());
        Response completeResponse = Response.fromStream(in);

        if (completeResponse.getStatus() != StatusCode.OK) {
            System.err.println("[CLIENT] Erro ao finalizar upload");
            return completeResponse.getStatus();
        }

        // Step 4: Send the signature file init
        BodyJSON initSignatureBody = new BodyJSON();
        initSignatureBody.put("action", "signature_init");
        initSignatureBody.put("workspaceId", workspaceId);
        initSignatureBody.put("signatureFileName", signatureFile.getName());
        initSignatureBody.put("size", String.valueOf(signatureFile.length()));

        int totalSignatureChunks = (int) Math.ceil((double) signatureFile.length() / chunkSize);
        initSignatureBody.put("chunks", String.valueOf(totalSignatureChunks));

        Request initSingatureRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "uploadfiletoworkspace",
                initSignatureBody
        );

        out.write(initSingatureRequest.toByteArray());
        Response initSignatureResponse = Response.fromStream(in);
        //System.out.println("[CLIENT] Resposta de inicialização: " + initResponse);

        if (initResponse.getStatus() != StatusCode.OK) {
            System.err.println("[CLIENT] Erro ao inicializar upload");
            return initResponse.getStatus();
        }

        BodyJSON initSignatureResponseBody = initSignatureResponse.getBodyJSON();
        String signatureFileId = initSignatureResponseBody.get("signatureFileId");

        // Step 5: Send signature file chunks
        try (FileInputStream fileIn = new FileInputStream(signatureFile)) {
            byte[] buffer = new byte[chunkSize];
            int chunkId = 0;
            int bytesRead;

            while ((bytesRead = fileIn.read(buffer)) > 0) {
                byte[] chunkData;
                if (bytesRead < buffer.length) {
                    chunkData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                } else {
                    chunkData = buffer;
                }

                BodyRaw chunkBody = new BodyRaw(chunkData);
                Request chunkRequest = new Request(
                        NetworkUtils.randomUUID(),
                        BodyFormat.RAW,
                        "uploadfiletoworkspace",
                        chunkBody
                );
                chunkRequest.addHeader("SIGNATURE-FILE-ID", signatureFileId);
                chunkRequest.addHeader("CHUNK-ID", String.valueOf(chunkId));
                chunkRequest.addHeader("TYPE", "SIGNATURE-CHUNK");

                //System.out.println("[CLIENT] Enviando chunk " + (chunkId + 1) + "/" + (totalChunks));
                out.write(chunkRequest.toByteArray());

                Response chunkResponse = Response.fromStream(in);
                if (chunkResponse.getStatus() != StatusCode.OK) {
                    System.err.println("[CLIENT] Erro ao enviar chunk " + chunkId);
                    return chunkResponse.getStatus();
                }

                chunkId++;
            }
        }

        // Step 6: Complete the signature file upload
        BodyJSON completeSignatureBody = new BodyJSON();
        completeSignatureBody.put("action", "signature_complete");
        completeSignatureBody.put("fileId", fileId);
        completeSignatureBody.put("signatureFileId", signatureFileId);

        Request completeRSignatureRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "uploadfiletoworkspace",
                completeSignatureBody
        );

        out.write(completeRSignatureRequest.toByteArray());
        Response completeSignatureResponse = Response.fromStream(in);

        if (completeSignatureResponse.getStatus() != StatusCode.OK) {
            System.err.println("[CLIENT] Erro ao finalizar upload");
            return completeSignatureResponse.getStatus();
        }


        //System.out.println("[CLIENT] Ficheiro enviado com sucesso!");
        return completeSignatureResponse.getStatus();
    }

    /**
     * Receives a file from the server.
     *
     * @param fileName the file name
     * @param workspaceId the workspace ID
     * @param in the input stream
     * @param out the output stream
     */
    private static StatusCode receiveKeyFromServer(String fileName, String workspaceId, DataInputStream in, DataOutputStream out) throws IOException {
        // Step 1: Initialize the download
        BodyJSON initBody = new BodyJSON();
        initBody.put("action", "init");
        initBody.put("workspaceId", workspaceId);

        Request initRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "downloadkeyfromworkspace",
                initBody
        );

        out.write(initRequest.toByteArray());
        Response initResponse = Response.fromStream(in);
        //System.out.println("[CLIENT] Resposta de inicialização: " + initResponse);

        if (initResponse.getStatus() != StatusCode.OK) {
            //System.err.println("[CLIENT] Erro ao inicializar download");
            return StatusCode.NOT_FOUND;
        }

        BodyJSON initResponseBody = initResponse.getBodyJSON();
        int totalChunks = Integer.parseInt(initResponseBody.get("chunks"));
        int fileSize = Integer.parseInt(initResponseBody.get("size"));
        String fileId = initResponseBody.get("fileId");

        // Step 2: Receive file chunks
        try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
            for (int chunkId = 0; chunkId < totalChunks; chunkId++) {
                BodyJSON chunkBody = new BodyJSON();
                chunkBody.put("action", "chunk");
                chunkBody.put("chunkId", String.valueOf(chunkId));
                chunkBody.put("fileId", fileId);

                Request chunkRequest = new Request(
                        NetworkUtils.randomUUID(),
                        BodyFormat.JSON,
                        "downloadkeyfromworkspace",
                        chunkBody
                );

                out.write(chunkRequest.toByteArray());
                Response chunkResponse = Response.fromStream(in);
                if (!String.valueOf(chunkId).equals(chunkResponse.getHeader("CHUNK-ID"))) {
                    System.err.println("[CLIENT] Erro ao receber chunk " + chunkId);
                    Files.deleteIfExists(Paths.get(fileName));
                    return chunkResponse.getStatus();
                }

                if (!fileId.equals(chunkResponse.getHeader("FILE-ID"))) {
                    System.err.println("[CLIENT] Erro ao receber chunk " + chunkId);
                    Files.deleteIfExists(Paths.get(fileName));
                    return chunkResponse.getStatus();
                }

                //System.out.println("[CLIENT] Resposta de chunk " + chunkId + ": " + chunkResponse);

                if (chunkResponse.getStatus() != StatusCode.OK) {
                    System.err.println("[CLIENT] Erro ao receber chunk " + chunkId);
                    Files.deleteIfExists(Paths.get(fileName));
                    return chunkResponse.getStatus();
                }

                BodyRaw chunkData = chunkResponse.getBodyRaw();
                fileOut.write(chunkData.toBytes());
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Erro ao receber ficheiro: " + e.getMessage());
            Files.deleteIfExists(Paths.get(fileName));
            return StatusCode.NOK;
        }

        // Step 3: Complete the download
        BodyJSON completeBody = new BodyJSON();
        completeBody.put("action", "complete");
        completeBody.put("fileId", fileId);

        Request completeRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "downloadkeyfromworkspace",
                completeBody
        );

        out.write(completeRequest.toByteArray());
        Response completeResponse = Response.fromStream(in);

        if (completeResponse.getStatus() != StatusCode.OK) {
            System.err.println("[CLIENT] Erro ao finalizar download!");
            Files.deleteIfExists(Paths.get(fileName));

            return completeResponse.getStatus();
        }

        //System.out.println("[CLIENT] Ficheiro recebido com sucesso!");
        return completeResponse.getStatus();
    }

    /**
     * Sends a file to the server.
     *
     * @param filePath the file path
     * @param in the input stream
     * @param out the output stream
     */
    private static String sendKeyToServer(String filePath, String workspaceId, DataInputStream in, DataOutputStream out) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            //System.err.println("[CLIENT] Ficheiro não encontrado: " + filePath);
//            return StatusCode.NOT_FOUND;
            return null;
        }

        // Step 1: Initialize the upload
        //System.out.println("[CLIENT] Iniciando envio do ficheiro: " + file.getName());

        BodyJSON initBody = new BodyJSON();
        initBody.put("action", "init");
        initBody.put("workspaceId", workspaceId);
        initBody.put("size", String.valueOf(file.length()));

        int chunkSize = 1024 * 64; // 64KB chunks
        int totalChunks = (int) Math.ceil((double) file.length() / chunkSize);
        initBody.put("chunks", String.valueOf(totalChunks));

        Request initRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "uploadkeytoworkspace",
                initBody
        );

        out.write(initRequest.toByteArray());
        Response initResponse = Response.fromStream(in);
        //System.out.println("[CLIENT] Resposta de inicialização: " + initResponse);

        if (initResponse.getStatus() != StatusCode.OK) {
            System.err.println("[CLIENT] Erro ao inicializar upload");
//            return initResponse.getStatus();
            return null;
        }

        BodyJSON initResponseBody = initResponse.getBodyJSON();
        String fileId = initResponseBody.get("fileId");

        // Step 2: Send file chunks
        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[chunkSize];
            int chunkId = 0;
            int bytesRead;

            while ((bytesRead = fileIn.read(buffer)) > 0) {
                byte[] chunkData;
                if (bytesRead < buffer.length) {
                    chunkData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                } else {
                    chunkData = buffer;
                }

                BodyRaw chunkBody = new BodyRaw(chunkData);
                Request chunkRequest = new Request(
                        NetworkUtils.randomUUID(),
                        BodyFormat.RAW,
                        "uploadkeytoworkspace",
                        chunkBody
                );
                chunkRequest.addHeader("FILE-ID", fileId);
                chunkRequest.addHeader("CHUNK-ID", String.valueOf(chunkId));
                chunkRequest.addHeader("TYPE", "CHUNK");

                //System.out.println("[CLIENT] Enviando chunk " + (chunkId + 1) + "/" + (totalChunks));
                out.write(chunkRequest.toByteArray());

                Response chunkResponse = Response.fromStream(in);
                if (chunkResponse.getStatus() != StatusCode.OK) {
                    System.err.println("[CLIENT] Erro ao enviar chunk " + chunkId);
//                    return chunkResponse.getStatus();
                    return null;
                }

                chunkId++;
            }
        }

        // Step 3: Complete the upload
        BodyJSON completeBody = new BodyJSON();
        completeBody.put("action", "complete");
        completeBody.put("fileId", fileId);

        Request completeRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "uploadkeytoworkspace",
                completeBody
        );

        out.write(completeRequest.toByteArray());
        Response completeResponse = Response.fromStream(in);

        if (completeResponse.getStatus() != StatusCode.OK) {
            System.err.println("[CLIENT] Erro ao finalizar upload");
//            return completeResponse.getStatus();
            return null;
        }

        //System.out.println("[CLIENT] Ficheiro enviado com sucesso!");
//        return completeResponse.getStatus();

        return fileId;
    }
}