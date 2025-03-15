package server.routes;

import server.models.*;
import server.utils.NetworkUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UploadWorkspaceHandler implements RouteHandler {
    @Override
    public Response handle(Request request) {
        if (request.getFormat() != BodyFormat.JSON) {
            return NetworkUtils.createErrorResponse(request, "Request inválido");
        }

        BodyJSON body = (BodyJSON) request.getBody();
        String action = body.get("action");

        switch (action) {
            case "attachFile":
                return handleAttachFile(request, body);
            default:
                return NetworkUtils.createErrorResponse(request, "Ação desconhecida: " + action);
        }
    }

    private Response handleAttachFile(Request request, BodyJSON body) {
        String workspaceId = body.get("workspaceId");
        String tempFileId = body.get("tempFileId");

        return new Response(
            request.getUUID(),
            BodyFormat.JSON,
            StatusCodes.NOT_FOUND,
            null
        );
    }
}
