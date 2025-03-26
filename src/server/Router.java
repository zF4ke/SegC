package server;

import server.models.*;
import server.routes.*;

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
        this.authenticatedUser = authenticatedUser;
        this.in = in;
        this.out = out;
    }

    public void handleRequests() {
        System.out.println("\n[ROUTER] A aguardar por requests...");

        try {
            while (!clientSocket.isClosed()) {
                try {
                    Request request = Request.fromStream(in);
                    //request.addHeader("USER-ID", authenticatedUser.getUserId());
                    request.setAuthenticatedUser(authenticatedUser);

                    System.out.println("[ROUTER] Request recebido de " + request.getAuthenticatedUser().getUserId() + ": " + request);

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
            case "createworkspace":
                return new CreateWorkspaceHandler().handle(request);
            case "addusertoworkspace":
                return new AddUserToWorkspaceHandler().handle(request);
            case "uploadfiletoworkspace":
                return new UploadFileToWorkspaceHandler().handle(request);
            case "downloadfilefromworkspace":
                return new DownloadFileFromWorkspaceHandler().handle(request);
//            case "removefilefromworkspace":
//                return new RemoveFileFromWorkspaceHandler().handle(request);
            case "listworkspaces":
                return new ListWorkspacesHandler().handle(request);
            case "listworkspacefiles":
                return new ListWorkspaceFilesHandler().handle(request);
            default:
                BodyJSON body = new BodyJSON();
                body.put("error", "Rota não encontrada");

                return new Response(request.getUUID(), StatusCode.NOT_FOUND, BodyFormat.JSON, body);
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
