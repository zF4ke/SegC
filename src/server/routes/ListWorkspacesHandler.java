package server.routes;

import server.WorkspaceManager;
import server.models.*;
import server.utils.NetworkUtils;

import java.util.Arrays;

public class ListWorkspacesHandler implements RouteHandler {

    @Override
    public Response handle(Request request) {
        try {
            User user = request.getAuthenticatedUser();
            WorkspaceManager workspaceManager = WorkspaceManager.getInstance();
            String[] workspaceIds = workspaceManager.listWorkspaces(user.getUserId());

            BodyJSON responseBody = new BodyJSON();
            responseBody.put("workspaceIds", Arrays.toString(workspaceIds));

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
