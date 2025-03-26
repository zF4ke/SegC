package server.routes;

import server.WorkspaceManager;
import server.models.*;
import server.utils.NetworkUtils;

import java.util.Arrays;

public class ListWorkspaceFilesHandler implements RouteHandler {

    @Override
    public Response handle(Request request) {
        try {
            User user = request.getAuthenticatedUser();
            WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
            BodyJSON body = request.getBodyJSON();
            String workspaceId = body.get("workspaceId");

            if (!workspaceManager.isUserInWorkspace(user.getUserId(), workspaceId)) {
                return NetworkUtils.createErrorResponse(request, StatusCode.NOPERM);
            }

            String[] files = workspaceManager.listWorkspaceFiles(user.getUserId(), workspaceId);
            BodyJSON responseBody = new BodyJSON();
            responseBody.put("files", Arrays.toString(files));

            return new Response(
                    request.getUUID(),
                    StatusCode.OK,
                    BodyFormat.JSON,
                    responseBody
            );
        } catch (Exception e) {
            return NetworkUtils.createErrorResponse(request, "Erro ao listar workspaces: " + e.getMessage());
        }
    }
}
