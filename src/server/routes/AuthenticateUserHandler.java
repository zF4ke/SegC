package server.routes;

import server.models.*;

public class AuthenticateUserHandler implements RouteHandler {

    @Override
    public Response handle(Request request) {
        Body body = request.getBody();
        if (body.getFormat() != BodyFormat.JSON) {
            return new Response(request.getUUID(), BodyFormat.JSON, StatusCodes.BAD_REQUEST, null);
        }

        BodyJSON json = (BodyJSON) request.getBody();
        String username = json.get("username");
        String password = json.get("password");

        // ...
        // Verifica se o usuário e senha são válidos
        // Bla bla bla

        System.out.println("Usuário: " + username);
        System.out.println("Senha: " + password);

        BodyJSON responseBody = new BodyJSON();
        responseBody.put("message", "logado com sucesso!");

        return new Response(request.getUUID(), BodyFormat.JSON, StatusCodes.OK, responseBody);
    }
}
