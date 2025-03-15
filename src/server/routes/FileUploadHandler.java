package server.routes;

import server.models.*;
import server.utils.NetworkUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class FileUploadHandler implements RouteHandler {
    private static final String TEMP_DIR = "data/temp_files";
    private static final Map<String, FileUploadSession> uploadSessions = new HashMap<>();

    public FileUploadHandler() {
        // Criar diretório temporário
        new File(TEMP_DIR).mkdirs();
        this.cleanupOrphanedFiles();
    }

    @Override
    public Response handle(Request request) {
        if (request.getFormat() == BodyFormat.JSON) {
            BodyJSON body = (BodyJSON) request.getBody();
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
    }

    /**
     * Handles the initialization of a file upload.
     *
     * @param request the request
     * @return the response
     */
    private Response handleInitialization(Request request) {
        BodyJSON body = (BodyJSON) request.getBody();

        String fileName = body.get("fileName");
        long fileSize = Long.parseLong(body.get("size"));
        int chunks = Integer.parseInt(body.get("chunks"));

        if (fileName == null || fileSize < 0 || chunks <= 0) {
            return NetworkUtils.createErrorResponse(request, "Parâmetros inválidos");
        }

        String fileId = java.util.UUID.randomUUID().toString();
        String tempFilePath = TEMP_DIR + File.separator + fileId;

        FileUploadSession session = new FileUploadSession(fileId, fileName, fileSize, tempFilePath);
        uploadSessions.put(fileId, session);

        BodyJSON responseBody = new BodyJSON();
        responseBody.put("fileId", fileId);
        responseBody.put("status", "ready");

        return new Response(request.getUUID(), BodyFormat.JSON, StatusCodes.OK, responseBody);
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

        if (fileId == null) {
            return NetworkUtils.createErrorResponse(request, "FILE-ID não fornecido");
        }

        FileUploadSession session = uploadSessions.get(fileId);
        if (session == null) {
            return NetworkUtils.createErrorResponse(request, "Sessão de upload não encontrada");
        }

        if (session.isComplete) {
            return NetworkUtils.createErrorResponse(request, "Upload já foi concluído");
        }

        if (chunkId != session.nextExpectedChunk) {
            return NetworkUtils.createErrorResponse(request,
                    "CHUNK-ID inválido, esperado: " + session.nextExpectedChunk);
        }

        try {
            byte[] data = ((BodyRaw) request.getBody()).toBytes();
            session.file.seek(session.receivedBytes);
            session.file.write(data);
            session.receivedBytes += data.length;
            session.nextExpectedChunk++;

            BodyJSON responseBody = new BodyJSON();
            responseBody.put("fileId", fileId);
            responseBody.put("chunkId", String.valueOf(chunkId));
            responseBody.put("status", "chunk received");

            return new Response(request.getUUID(), BodyFormat.JSON, StatusCodes.OK, responseBody);
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
        BodyJSON body = (BodyJSON) request.getBody();
        String fileId = body.get("fileId");

        if (fileId == null) {
            return NetworkUtils.createErrorResponse(request, "fileId não fornecido");
        }

        FileUploadSession session = uploadSessions.get(fileId);
        if (session == null) {
            return NetworkUtils.createErrorResponse(request, "Sessão de upload não encontrada");
        }

        try {
            session.file.close();
            session.isComplete = true;

            // Aqui poderia mover o arquivo para alguma localização permanente
            // rename the file to the fileName just for testing purposes
            File file = new File(session.tempFilePath);
            file.renameTo(new File("data/teste/" + session.fileId + session.fileName));

            BodyJSON responseBody = new BodyJSON();
            responseBody.put("fileId", fileId);
            responseBody.put("status", "file uploaded");

            return new Response(request.getUUID(), BodyFormat.JSON, StatusCodes.OK, responseBody);
        } catch (Exception e) {
            return NetworkUtils.createErrorResponse(request, "Erro ao finalizar upload: " + e.getMessage());
        }
    }

    /**
     * Removes files that are not part of an active upload session. (Orphaned files)
     */
    private void cleanupOrphanedFiles() {
        File tempDir = new File(TEMP_DIR);
        if (tempDir.exists()) {
            File[] tempFiles = tempDir.listFiles();
            if (tempFiles != null) {
                for (File file : tempFiles) {
                    if (file.isFile() && !isActiveUpload(file.getName())) {
                        file.delete();
                        System.out.println("[FileUploadHandler] Removido ficheiro órfão: " + file.getPath());
                    }
                }
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
    private static class FileUploadSession {
        private final String fileId;
        private final String fileName;
        private final long totalSize;
        private final String tempFilePath;
        private long receivedBytes = 0;
        private int nextExpectedChunk = 0;
        private RandomAccessFile file;
        private boolean isComplete = false;
        private User owner = null;

        public FileUploadSession(String fileId, String fileName, long totalSize, String tempFilePath) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.totalSize = totalSize;
            this.tempFilePath = tempFilePath;
            try {
                this.file = new RandomAccessFile(tempFilePath, "rw");
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Falha ao criar ficheiro temporário", e);
            }
        }
    }
}
