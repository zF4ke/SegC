package client;

import server.models.*;
import server.utils.NetworkUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    public void createWorkspace(String workspaceName) {
        BodyJSON body = new BodyJSON();
        body.put("workspaceName", workspaceName);

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
    public void addUserToWorkspace(String user, String workspaceId) {
        BodyJSON body = new BodyJSON();
        body.put("user", user);
        body.put("workspaceId", workspaceId);

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
    }

    /**
     * Sends a request to the server to upload files to a workspace.
     *
     * @param workspaceId the workspace ID
     * @param files the files
     */
    public void uploadFilesToWorkspace(String workspaceId, String[] files) {
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
                StatusCode status = sendFileToServer(file, workspaceId, in, out);

                //System.out.print("\t" + file + ": " + status);
                if (!first) {
                    System.out.println("\t\t  " + file + ": " + status);
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
     * Sends a request to the server to download files from a workspace.
     *
     * @param workspaceId the workspace id
     * @param files the files
     */
    public void downloadFilesFromWorkspace(String workspaceId, String[] files) {
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
                StatusCode status = receiveFileFromServer(file, workspaceId, in, out);
                if (!first) {
                    System.out.println("\t\t  " + file + ": " + status);
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
                        System.out.println("\t\t  " + file + ": " + response.getStatus());
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
    private static StatusCode receiveFileFromServer(String fileName, String workspaceId, DataInputStream in, DataOutputStream out) throws IOException {
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
    private static StatusCode sendFileToServer(String filePath, String workspaceId, DataInputStream in, DataOutputStream out) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            //System.err.println("[CLIENT] Ficheiro não encontrado: " + filePath);
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

        // Step 3: Complete the upload
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

        //System.out.println("[CLIENT] Ficheiro enviado com sucesso!");
        return completeResponse.getStatus();
    }
}