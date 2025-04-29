package server.routes;

import server.WorkspaceManager;
import server.models.*;
import server.utils.InputUtils;
import server.utils.NetworkUtils;

import java.io.File;
import java.nio.file.Files;

public class AddUserToWorkspaceHandler implements RouteHandler {

    @Override
    public Response handle(Request request) {
        try {
            User user = request.getAuthenticatedUser();
            BodyJSON body = request.getBodyJSON();
            String userToAdd = body.get("user");
            String workspaceId = body.get("workspaceId");
            String keyFileId = body.get("keyFileId");

            if (!InputUtils.isValidUserId(userToAdd) || !InputUtils.isValidWorkspaceId(workspaceId)) {
                return NetworkUtils.createErrorResponse(request, "Parâmetros inválidos.");
            }

            WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
            StatusCode status = workspaceManager.addUserToWorkspace(user.getUserId(), userToAdd, workspaceId);

            if (status == StatusCode.OK) {
                UploadKeyToWorkspaceHandler.FileUploadSession session = UploadKeyToWorkspaceHandler.getUploadSession(keyFileId);
                if (session == null) {
                    return NetworkUtils.createErrorResponse(request, "Sessão de upload inválida.");
                }

                String fileName = session.getFileName();
                String filePath = session.getTempFilePath();

                File file = new File(filePath);

                boolean success = workspaceManager.uploadFile(user.getUserId(), workspaceId, file, fileName);
                if (!success) {
                    return NetworkUtils.createErrorResponse(request, "Erro ao mover ficheiro para o workspace");
                }

                // remove the temporary file
                UploadKeyToWorkspaceHandler.removeFileUploadSession(keyFileId);

                BodyJSON responseBody = new BodyJSON();
                responseBody.put("message", "Usuário adicionado ao workspace com sucesso.");

                return new Response(
                        request.getUUID(),
                        status,
                        BodyFormat.JSON,
                        responseBody
                );
            } else {
                return NetworkUtils.createErrorResponse(request, status);
            }
        } catch (Exception e) {
            return NetworkUtils.createErrorResponse(request, "Erro ao adicionar usuário ao workspace: " + e.getMessage());
        }
    }
}
