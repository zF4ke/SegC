package server.routes;

import server.WorkspaceManager;
import server.models.*;
import server.utils.NetworkUtils;

public class CreateWorkspaceHandler implements RouteHandler {

    @Override
    public Response handle(Request request) {
        try {
            User user = request.getAuthenticatedUser();
            BodyJSON body = request.getBodyJSON();
            String workspaceName = body.get("workspaceName");

            WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
            StatusCode status = workspaceManager.createWorkspace(user.getUserId(), workspaceName);

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
