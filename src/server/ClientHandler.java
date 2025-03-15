package server;

import client.ClientRouter;
import server.models.*;
import server.utils.NetworkUtils;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;

    private final AuthenticationManager authManager;
    private User authenticatedUser;


    /**
     * Create a new client handler.
     *
     * @param clientSocket the client socket
     */
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.authManager = AuthenticationManager.getInstance();
    }

    @Override
    public void run() {
        System.out.println("[SERVER] Cliente conectado: " + clientSocket);

        this.openStreams();

        if (!authenticateClient()) {
            System.out.println("[SERVER] Autenticação falhou. A fechar a ligação.");
            this.closeSocket();
            return;
        }

        System.out.println("[SERVER] Autenticação bem sucedida.");

        Router router = new Router(clientSocket, in, out, authenticatedUser);
        router.handleRequests();
    }

    private void openStreams() {
        try {
            this.in = new DataInputStream(clientSocket.getInputStream());
            this.out = new DataOutputStream(clientSocket.getOutputStream());

            System.out.println("[SERVER] Streams abertas.");
        } catch (IOException e) {
            System.err.println("[SERVER] Erro ao abrir streams: " + e.getMessage());
        }
    }

    /**
     * Authenticate the client.
     *
     * @return true if the client is authenticated, false otherwise
     */
    private boolean authenticateClient() {
        try {
            Request request = Request.fromStream(in);
            Body body = request.getBody();

            if (body.getFormat() != BodyFormat.JSON) {
                this.createErrorAuthResponse(StatusCodes.BAD_REQUEST);
                return false;
            }

            BodyJSON json = (BodyJSON) body;
            String userid = json.get("userid");
            String password = json.get("password");

            //System.out.println(request);
            System.out.println("[SERVER] Autenticar cliente: " + userid);

            int status = authManager.authenticate(userid, password);
            if (status != StatusCodes.OK) {
                this.createErrorAuthResponse(status);
                return false;
            }

            this.authenticatedUser = authManager.getUser(userid);

            Response response = new Response(request.getUUID(), BodyFormat.JSON, status, new BodyJSON());
            //System.out.println(response);
            this.out.write(response.toByteArray());

            return true;
        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao autenticar cliente: " + e.getMessage());
        }

        return false;
    }

    private void createErrorAuthResponse(int status) {
        try {
            Response response = new Response(NetworkUtils.randomUUID(), BodyFormat.JSON, status, new BodyJSON());
            this.out.write(response.toByteArray());
        } catch (IOException e) {
            System.err.println("[SERVER] Erro ao criar resposta de autenticação: " + e.getMessage());
        }
    }

    private void closeSocket() {
        try {
            clientSocket.close();
        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao fechar socket: " + e.getMessage());
        }
    }
}
