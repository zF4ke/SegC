package server.routes;

import server.WorkspaceManager;
import server.models.*;
import server.utils.InputUtils;
import server.utils.NetworkUtils;

public class CreateWorkspaceHandler implements RouteHandler {

    @Override
    public Response handle(Request request) {
        try {
            User user = request.getAuthenticatedUser();
            BodyJSON body = request.getBodyJSON();
            String workspaceName = body.get("workspaceName");
            String workspacePassword = body.get("workspacePassword");

            if (!InputUtils.isValidWorkspaceId(workspaceName)) {
                return NetworkUtils.createErrorResponse(request, "Nome do workspace inválido.");
            }

            WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
            StatusCode status = workspaceManager.createWorkspace(user.getUserId(), workspaceName, workspacePassword);

            if (status == StatusCode.OK) {
                BodyJSON responseBody = new BodyJSON();
                responseBody.put("message", "Workspace criado com sucesso.");

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
            return NetworkUtils.createErrorResponse(request, "Erro ao criar workspace: " + e.getMessage());
        }
    }
}
