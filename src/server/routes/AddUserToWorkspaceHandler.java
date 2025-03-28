package server.routes;

import server.WorkspaceManager;
import server.models.*;
import server.utils.InputUtils;
import server.utils.NetworkUtils;

public class AddUserToWorkspaceHandler implements RouteHandler {

    @Override
    public Response handle(Request request) {
        try {
            User user = request.getAuthenticatedUser();
            BodyJSON body = request.getBodyJSON();
            String userToAdd = body.get("user");
            String workspaceId = body.get("workspaceId");

            if (!InputUtils.isValidUserId(userToAdd) || !InputUtils.isValidWorkspaceId(workspaceId)) {
                return NetworkUtils.createErrorResponse(request, "Parâmetros inválidos.");
            }

            WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
            StatusCode status = workspaceManager.addUserToWorkspace(user.getUserId(), userToAdd, workspaceId);

            if (status == StatusCode.OK) {
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
