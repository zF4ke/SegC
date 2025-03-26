package server.routes;

import server.models.*;
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
import java.util.stream.Stream;

public class FileUploadHandler implements RouteHandler {
    private static final String TEMP_DIR = "data/temp_files";
    private static final Map<String, FileUploadSession> uploadSessions = new HashMap<>();

    /**
     * Creates a new file upload handler.
     */
    public FileUploadHandler() {
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
     * Handles the initialization of a file upload.
     *
     * @param request the request
     * @return the response
     */
    private Response handleInitialization(Request request) {
        BodyJSON body = request.getBodyJSON();

        String fileName = body.get("fileName");
        long fileSize = Long.parseLong(body.get("size"));
        int chunks = Integer.parseInt(body.get("chunks"));
        User owner = request.getAuthenticatedUser();

        if (fileName == null || fileSize < 0 || chunks <= 0) {
            return NetworkUtils.createErrorResponse(request, "Parâmetros inválidos");
        }

        if (owner == null) {
            return NetworkUtils.createErrorResponse(request, "Utilizador não autenticado");
        }

        String fileId = java.util.UUID.randomUUID().toString();
        String tempFilePath = TEMP_DIR + File.separator + fileId;

        FileUploadSession session = new FileUploadSession(fileId, fileName, fileSize, tempFilePath, owner.getUserId());
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
        User owner = request.getAuthenticatedUser();

        if (fileId == null) {
            return NetworkUtils.createErrorResponse(request, "FILE-ID não fornecido");
        }

        if (owner == null) {
            return NetworkUtils.createErrorResponse(request, "Utilizador não autenticado");
        }

        FileUploadSession session = uploadSessions.get(fileId);
        if (session == null) {
            return NetworkUtils.createErrorResponse(request, "Sessão de upload não encontrada");
        }

        if (!session.isOwner(owner)) {
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

        if (fileId == null) {
            return NetworkUtils.createErrorResponse(request, "fileId não fornecido");
        }

        FileUploadSession session = uploadSessions.get(fileId);
        if (session == null) {
            return NetworkUtils.createErrorResponse(request, "Sessão de upload não encontrada");
        }

        User owner = request.getAuthenticatedUser();
        if (owner == null) {
            return NetworkUtils.createErrorResponse(request, "Utilizador não autenticado");
        }

        if (!session.isOwner(owner)) {
            return NetworkUtils.createErrorResponse(request, "Utilizador não tem permissão para fazer upload deste ficheiro");
        }

        try {
            session.file.close();
            session.isComplete = true;

            BodyJSON responseBody = new BodyJSON();
            responseBody.put("fileId", fileId);
            responseBody.put("status", "file uploaded");

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

        public FileUploadSession(
                String fileId,
                String fileName,
                long totalSize,
                String tempFilePath,
                String ownerUserId
        ) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.totalSize = totalSize;
            this.tempFilePath = tempFilePath;
            this.ownerUserId = ownerUserId;

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

        /**
         * Gets the file ID.
         *
         * @return the file ID
         */
        public String getFileId() {
            return fileId;
        }

        /**
         * Gets the file name.
         *
         * @return the file name
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * Gets the total size of the file.
         *
         * @return the total size
         */
        public long getTotalSize() {
            return totalSize;
        }

        /**
         * Gets the path to the temporary file.
         *
         * @return the path to the temporary file
         */
        public String getTempFilePath() {
            return tempFilePath;
            }
    }

    /**
     * Gets an upload session by file ID.
     *
     * @param fileId the file ID
     * @return the upload session
     */
    public static FileUploadSession getUploadSession(String fileId) {
        return uploadSessions.get(fileId);
    }

    /**
     * Removes an upload session by file ID.
     *
     * @param fileId the file ID
     */
    public static void removeUploadSession(String fileId) {
        uploadSessions.remove(fileId);
    }

    /**
     * Gets the temporary directory for file uploads.
     *
     * @return the temporary directory
     */
    public static String getTempDir() {
        return TEMP_DIR;
    }
}
