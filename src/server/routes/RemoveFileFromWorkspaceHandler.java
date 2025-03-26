package server.routes;

import server.WorkspaceManager;
import server.models.*;
import server.utils.NetworkUtils;


public class RemoveFileFromWorkspaceHandler implements RouteHandler {
    @Override
    public Response handle(Request request) {
        try {
            User user = request.getAuthenticatedUser();
            WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
            BodyJSON body = request.getBodyJSON();
            String workspaceId = body.get("workspaceId");
            String fileName = body.get("fileName");

            if (!workspaceManager.isUserInWorkspace(user.getUserId(), workspaceId)) {
                return NetworkUtils.createErrorResponse(request, StatusCode.NOPERM);
            }

            StatusCode status = workspaceManager.removeFileFromWorkspace(user.getUserId(), workspaceId, fileName);
            if (status == StatusCode.OK) {
                BodyJSON responseBody = new BodyJSON();
                responseBody.put("message", "Ficheiro removido do workspace com sucesso.");

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
            return NetworkUtils.createErrorResponse(request, "Erro ao remover ficheiro do workspace: " + e.getMessage());
        }
    }
}
