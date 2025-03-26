package server.routes;

import server.WorkspaceManager;
import server.models.*;
import server.utils.InputUtils;
import server.utils.NetworkUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class UploadFileToWorkspaceHandler implements RouteHandler {
    private static final String TEMP_DIR = "data/temp_files";
    private static final Map<String, FileUploadSession> uploadSessions = new HashMap<>();

    public UploadFileToWorkspaceHandler() {
        try {
            Files.createDirectories(Paths.get(TEMP_DIR));
            this.cleanupOrphanedFiles();
        } catch (IOException e) {
            System.err.println("[FILE UPLOAD HANDLER] Erro ao criar diretório temporário: " + e.getMessage());
        }
    }

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
                    case "complete":
                        return handleCompletion(request);
                    default:
                        return NetworkUtils.createErrorResponse(request, "Ação inválida");
                }
            } else if (request.getFormat() == BodyFormat.RAW) {
                if (request.getHeader("TYPE").equals("CHUNK")) {
                    return handleChunkData(request);
                }
            }

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
        responseBody.put("message", "O utilizador tem permissão para fazer upload de ficheiros");

        return new Response(request.getUUID(), StatusCode.OK, BodyFormat.JSON, responseBody);
    }

    /**
     * Handles the initialization of a file upload.
     *
     * @param request the request
     * @return the response
     */
    private Response handleInitialization(Request request) {
        User user = request.getAuthenticatedUser();
        WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
        BodyJSON body = request.getBodyJSON();
        String workspaceId = body.get("workspaceId");

        if (user == null || !workspaceManager.isUserInWorkspace(user.getUserId(), workspaceId)) {
            return NetworkUtils.createErrorResponse(request, StatusCode.NOPERM);
        }

        String fileName = body.get("fileName");
        long fileSize = Long.parseLong(body.get("size"));
        int chunks = Integer.parseInt(body.get("chunks"));

        if (fileName == null || fileSize < 0 || chunks <= 0 || !InputUtils.isValidFilename(fileName)) {
            return NetworkUtils.createErrorResponse(request, "Parâmetros inválidos");
        }

        String fileId = UUID.randomUUID().toString();
        String tempFilePath = TEMP_DIR + File.separator + fileId;

        FileUploadSession session = new FileUploadSession(
                fileId,
                fileName,
                fileSize,
                tempFilePath,
                user.getUserId(),
                workspaceId
        );
        uploadSessions.put(fileId, session);

        BodyJSON responseBody = new BodyJSON();
        responseBody.put("fileId", fileId);
        responseBody.put("status", "ready");

        return new Response(request.getUUID(), StatusCode.OK, BodyFormat.JSON, responseBody);
    }

    /**
     * Handles a chunk of data from the client.
     *
     * @param request the request
     * @return the response
     */
    private Response handleChunkData(Request request) {
        String fileId = request.getHeader("FILE-ID");
        int chunkId = Integer.parseInt(request.getHeader("CHUNK-ID"));
        User user = request.getAuthenticatedUser();

        if (fileId == null) {
            return NetworkUtils.createErrorResponse(request, "FILE-ID não fornecido");
        }
        if (user == null) {
            return NetworkUtils.createErrorResponse(request, "Utilizador não autenticado");
        }

        FileUploadSession session = uploadSessions.get(fileId);
        if (session == null) {
            return NetworkUtils.createErrorResponse(request, "Sessão de upload não encontrada");
        }
        WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
        if (!session.isOwner(user) ||
                !workspaceManager.isUserInWorkspace(user.getUserId(), session.workspaceId)) {
            return NetworkUtils.createErrorResponse(request, "Utilizador não tem permissão para fazer upload deste ficheiro");
        }
        if (session.isComplete) {
            return NetworkUtils.createErrorResponse(request, "Upload já foi concluído");
        }

        if (chunkId != session.nextExpectedChunk) {
            return NetworkUtils.createErrorResponse(request,
                    "CHUNK-ID inválido, esperado: " + session.nextExpectedChunk);
        }

        try {
            BodyRaw body = request.getBodyRaw();
            byte[] data = body.toBytes();

            session.file.seek(session.receivedBytes);
            session.file.write(data);
            session.receivedBytes += data.length;
            session.nextExpectedChunk++;

            BodyJSON responseBody = new BodyJSON();
            responseBody.put("fileId", fileId);
            responseBody.put("chunkId", String.valueOf(chunkId));
            responseBody.put("status", "chunk received");

            return new Response(request.getUUID(), StatusCode.OK, BodyFormat.JSON, responseBody);
        } catch (Exception e) {
            return NetworkUtils.createErrorResponse(request, "Erro ao processar chunk: " + e.getMessage());
        }
    }

    /**
     * Handles the completion of a file upload.
     *
     * @param request the request
     * @return the response
     */
    private Response handleCompletion(Request request) {
        BodyJSON body = request.getBodyJSON();
        String fileId = body.get("fileId");

        User user = request.getAuthenticatedUser();
        if (fileId == null) {
            return NetworkUtils.createErrorResponse(request, "fileId não fornecido");
        }
        if (user == null) {
            return NetworkUtils.createErrorResponse(request, "Utilizador não autenticado");
        }

        FileUploadSession session = uploadSessions.get(fileId);
        if (session == null) {
            return NetworkUtils.createErrorResponse(request, "Sessão de upload não encontrada");
        }
        WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
        if (!session.isOwner(user) ||
                !workspaceManager.isUserInWorkspace(user.getUserId(), session.workspaceId)) {
            return NetworkUtils.createErrorResponse(request, "Utilizador não tem permissão para fazer upload deste ficheiro");
        }

        try {
            session.file.close();
            session.isComplete = true;

            BodyJSON responseBody = new BodyJSON();
            responseBody.put("fileId", fileId);
            responseBody.put("status", "file uploaded");

            // move file to workspace directory
            File file = new File(session.tempFilePath);
            String fileName = session.fileName;

            boolean success = workspaceManager.uploadFile(user.getUserId(), session.workspaceId, file, fileName);
            if (!success) {
                return NetworkUtils.createErrorResponse(request, "Erro ao mover ficheiro para o workspace");
            }

            uploadSessions.remove(fileId);

            // remove temp file just in case
            Files.deleteIfExists(file.toPath());

            return new Response(request.getUUID(), StatusCode.OK, BodyFormat.JSON, responseBody);
        } catch (Exception e) {
            return NetworkUtils.createErrorResponse(request, "Erro ao finalizar upload: " + e.getMessage());
        }
    }

    /**
     * Removes files that are not part of an active upload session. (Orphaned files)
     */
    private void cleanupOrphanedFiles() {
        Path tempDir = Paths.get(TEMP_DIR);
        if (Files.exists(tempDir)) {
            try (Stream<Path> pathStream = Files.list(tempDir)) {
                pathStream
                        .filter(Files::isRegularFile)
                        .filter(path -> !isActiveUpload(path.getFileName().toString()))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                                System.out.println("[FILE UPLOAD HANDLER] Ficheiro órfão removido: " + path);
                            } catch (IOException e) {
                                System.err.println("[FILE UPLOAD HANDLER] Erro ao remover ficheiro órfão: " + e.getMessage());
                            }
                        });
            } catch (IOException e) {
                System.err.println("[FILE UPLOAD HANDLER] Erro ao limpar ficheiros órfãos: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if a file is part of an active upload session.
     *
     * @param filename the filename
     * @return true if the file is part of an active upload session, false otherwise
     */
    private boolean isActiveUpload(String filename) {
        for (FileUploadSession session : uploadSessions.values()) {
            if (session.tempFilePath.endsWith(filename) && !session.isComplete) {
                return true;
            }
        }
        return false;
    }

    /**
     * Represents a file upload session.
     */
    public static class FileUploadSession {
        private final String fileId;
        private final String fileName;
        private final long totalSize;
        private final String tempFilePath;
        private long receivedBytes = 0;
        private int nextExpectedChunk = 0;
        private final RandomAccessFile file;
        private boolean isComplete = false;
        private String ownerUserId = null;
        private String workspaceId = null;

        public FileUploadSession(
                String fileId,
                String fileName,
                long totalSize,
                String tempFilePath,
                String ownerUserId,
                String workspaceId
        ) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.totalSize = totalSize;
            this.tempFilePath = tempFilePath;
            this.ownerUserId = ownerUserId;
            this.workspaceId = workspaceId;

            try {
                this.file = new RandomAccessFile(tempFilePath, "rw");
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Falha ao criar ficheiro temporário", e);
            }
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
