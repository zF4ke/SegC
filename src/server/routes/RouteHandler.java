package server.routes;

import server.models.Request;
import server.models.Response;

public interface RouteHandler {
    /**
     * Handles a request.
     *
     * @param request the request
     * @return the response
     */
    Response handle(Request request);
}
