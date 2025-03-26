package server.routes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import server.WorkspaceManager;
import server.models.BodyFormat;
import server.models.BodyJSON;
import server.models.BodyRaw;
import server.models.Request;
import server.models.Response;
import server.models.StatusCode;
import server.models.User;
import server.utils.NetworkUtils;

public class DownloadFileFromWorkspaceHandler implements RouteHandler{
    private static final Map<String, FileDownloadSession> downloadSessions = new HashMap<>();
    private static final int CHUNK_SIZE = 1024 * 64; //64 KB

    @Override
    public Response handle(Request request) {
        try {
             if (request.getFormat() == BodyFormat.JSON) {
                BodyJSON body = request.getBodyJSON();
                String action = body.get("action");

                switch (action) {
                    case "verify":
                        return handlePermVerification(request);
                    case "init":
                        return handleInitialization(request);
                    case "chunk":
                        return handleChunkData(request);
                    case "complete":
                        return handleCompletion(request);

                    default:
                        return NetworkUtils.createErrorResponse(request, "Ação inválida");
                }
            } else 
            return NetworkUtils.createErrorResponse(request, "Request inválido");
        } catch (Exception e) {
            return NetworkUtils.createErrorResponse(request, "Erro ao processar pedido: " + e.getMessage());
        }
        
    }

    /**
     * Handles the permission verification for a file upload.
     *
     * @param request the request
     * @return the response
     */
    private Response handlePermVerification(Request request) {
        User user = request.getAuthenticatedUser();
        WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
        BodyJSON body = request.getBodyJSON();
        String workspaceId = body.get("workspaceId");

        if (user == null || !workspaceManager.isUserInWorkspace(user.getUserId(), workspaceId)) {
            return NetworkUtils.createErrorResponse(request, StatusCode.NOPERM);
        }

        BodyJSON responseBody = new BodyJSON();
        responseBody.put("message", "O utilizador tem permissão para fazer download de ficheiros");

        return new Response(request.getUUID(), StatusCode.OK, BodyFormat.JSON, responseBody);
    }

    private Response handleInitialization(Request request) {
        WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
        User user = request.getAuthenticatedUser();
        BodyJSON body = request.getBodyJSON();

        String workspaceId = body.get("workspaceId");
        String filename = body.get("fileName");

        if (user == null || !workspaceManager.isUserInWorkspace(user.getUserId(), workspaceId)) {
            return NetworkUtils.createErrorResponse(request, StatusCode.NOPERM);
        }

        if (!workspaceManager.isFileInWorkspace(filename, workspaceId)) {
            return NetworkUtils.createErrorResponse(request, StatusCode.NOK);
        }

        File file = workspaceManager.getFile(filename, workspaceId);
        String fileId = UUID.randomUUID().toString();
 
        BodyJSON initBody = new BodyJSON();
        initBody.put("action", "init");
        initBody.put("fileId", fileId);
        initBody.put("size", String.valueOf(file.length()));

        int chunkSize = 1024 * 64; // 64KB chunks
        int totalChunks = (int) Math.ceil((double) file.length() / chunkSize);
        initBody.put("chunks", String.valueOf(totalChunks));

        FileDownloadSession session = new FileDownloadSession(fileId, filename, file.length(), file.getPath(), user.getUserId(), workspaceId);
        downloadSessions.put(fileId, session);

        return new Response(
            request.getUUID(),
            StatusCode.OK,
            BodyFormat.JSON,
            initBody);
    }

    private Response handleChunkData(Request request) {
        BodyJSON body = request.getBodyJSON();
        String fileId = body.get("fileId");
        User user = request.getAuthenticatedUser();
        int chunkId = Integer.parseInt(body.get("chunkId"));

        if (fileId == null) {
            return NetworkUtils.createErrorResponse(request, "FILE-ID não fornecido");
        }
        if (user == null) {
            return NetworkUtils.createErrorResponse(request, "Utilizador não autenticado");
        }

        FileDownloadSession session = downloadSessions.get(fileId);
        if (session == null) {
            return NetworkUtils.createErrorResponse(request, "Sessão de download não encontrada");
        }
        WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
        if (!session.isOwner(user) ||
                !workspaceManager.isUserInWorkspace(user.getUserId(), session.workspaceId)) {
            return NetworkUtils.createErrorResponse(request, "Utilizador não tem permissão para fazer download deste ficheiro");
        }
        if (session.isComplete) {
            return NetworkUtils.createErrorResponse(request, "Download já foi concluído");
        }

        if (chunkId != session.nextExpectedChunk) {
            return NetworkUtils.createErrorResponse(request,
                    "chunkId inválido, esperado: " + session.nextExpectedChunk);
        }

        // Step 2: Send file chunks
        File file = new File(session.filePath);
        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];

            // 1. Get the file
            // 2. Get the right chunk to send to the client (chunkId * CHUNK_SIZE), starting from 0
            // 3. Send the chunk to the client

            long offset = (long) chunkId * CHUNK_SIZE;
            fileIn.skip(offset);
            int bytesRead = fileIn.read(buffer);
            byte[] chunkData = new byte[bytesRead];
            System.arraycopy(buffer, 0, chunkData, 0, bytesRead);

            int totalChunks = (int) Math.ceil((double) file.length() / CHUNK_SIZE);
            session.nextExpectedChunk++;
            if (session.nextExpectedChunk == totalChunks) {
                session.isComplete = true;
            }
            BodyRaw chunkBody = new BodyRaw(chunkData);
            Response chunkResponse = new Response(
                    request.getUUID(),
                    StatusCode.OK,
                    BodyFormat.RAW,
                    chunkBody);
            chunkResponse.addHeader("FILE-ID", fileId);
            chunkResponse.addHeader("CHUNK-ID", String.valueOf(chunkId));
            chunkResponse.addHeader("TYPE", "CHUNK");

            System.out.println("[CLIENT] Enviando chunk " + (chunkId + 1) + "/" + (totalChunks));

            return chunkResponse;
        } catch (IOException e) {
            return NetworkUtils.createErrorResponse(request, "Erro ao enviar chunk");
        }
    }

    private Response handleCompletion(Request request) {
        BodyJSON body = request.getBodyJSON();
        String fileId = body.get("fileId");
        User user = request.getAuthenticatedUser();

        if (fileId == null) {
            return NetworkUtils.createErrorResponse(request, "FILE-ID não fornecido");
        }
        if (user == null) {
            return NetworkUtils.createErrorResponse(request, "Utilizador não autenticado");
        }
        
        FileDownloadSession session = downloadSessions.get(fileId);
        if (session == null) {
            return NetworkUtils.createErrorResponse(request, "Sessão de upload não encontrada");
        }
        WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
        if (!session.isOwner(user) ||
                !workspaceManager.isUserInWorkspace(user.getUserId(), session.workspaceId)) {
            return NetworkUtils.createErrorResponse(request, "Utilizador não tem permissão para fazer upload deste ficheiro");
        }

        BodyJSON completeBody = new BodyJSON();
        completeBody.put("action", "complete");
        completeBody.put("fileId", fileId);

        session.isComplete = true;
        downloadSessions.remove(fileId);

        return new Response(
                request.getUUID(),
                StatusCode.OK,
                BodyFormat.JSON,
                completeBody);
    }



    /**
     * Represents a file upload session.
     */
    private static class FileDownloadSession {
        private final String fileId;
        private final String fileName;
        private final long totalSize;
        private final String filePath;
        private long receivedBytes = 0;
        private int nextExpectedChunk = 0;
        private boolean isComplete = false;
        private String ownerUserId = null;
        private String workspaceId = null;

        public FileDownloadSession(
                String fileId,
                String fileName,
                long totalSize,
                String filePath,
                String ownerUserId,
                String workspaceId
        ) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.totalSize = totalSize;
            this.filePath = filePath;
            this.ownerUserId = ownerUserId;
            this.workspaceId = workspaceId;

        }

        /**
         * Checks if a user is the owner of the file upload session.
         *
         * @param user the user
         * @return true if the user is the owner, false otherwise
         */
        public boolean isOwner(User user) {
            if (ownerUserId == null) {
                return false;
            }

            return ownerUserId.equals(user.getUserId());
        }
    }
}