package server;

import server.models.*;
import server.routes.AuthenticateUserHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Router {
    private final Socket clientSocket;
    private final User authenticatedUser;
    private final DataInputStream in;
    private final DataOutputStream out;

    public Router(Socket clientSocket, DataInputStream in, DataOutputStream out, User authenticatedUser) {
        this.clientSocket = clientSocket;
        this.in = in;
        this.out = out;
        this.authenticatedUser = authenticatedUser;
    }

    public void handleRequests() {
        System.out.println("\n[ROUTER] A aguardar por requests...");

        try {
            while (!clientSocket.isClosed()) {
                Request request = null;
                try {
                    request = Request.fromStream(in);

                    System.out.println("[ROUTER] Request recebido de " + authenticatedUser.getUserId() + ": " + request);

                    Response response = handleRequest(request);
                    System.out.println("[ROUTER] Response: " + response);

                    out.write(response.toByteArray());
                } catch (IOException e) {
                    //System.err.println("[ROUTER] Erro ao processar pedido: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[ROUTER] Erro ao processar pedido: " + e.getMessage());
        }
    }

    /**
     * Routes the request to the appropriate handler.
     *
     * @param request the request
     * @return the response
     */
    private static Response handleRequest(Request request) {
        switch (request.getRoute()) {
            case "authenticate":
                return new AuthenticateUserHandler().handle(request);
            default:
                BodyJSON body = new BodyJSON();
                body.put("error", "Rota não encontrada");

                return new Response(request.getUUID(), BodyFormat.JSON, StatusCodes.NOT_FOUND, body);
        }
    }

    /**
     * Close the socket.
     */
    private void closeSocket() {
        try {
            clientSocket.close();
        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao fechar socket: " + e.getMessage());
        }
    }
}
