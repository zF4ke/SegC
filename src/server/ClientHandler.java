package server;

import client.ClientRouter;
import server.models.User;

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
            String credentials = in.readUTF();
            if (!credentials.contains(" ")) {
                out.writeUTF(ServerResponse.INVALID_AUTH.toString());
                out.flush();
                return false;
            }

            String[] parts = credentials.split(" ");
            String userId = parts[0];
            String password = parts[1];

            ServerResponse response = authManager.authenticate(userId, password);
            out.writeUTF(response.toString());
            out.flush();

            if (response == ServerResponse.OK) {
                this.authenticatedUser = authManager.getUser(userId);
                System.out.println("[SERVER] Cliente autenticado: " + this.authenticatedUser);
                return true;
            }

        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao autenticar cliente: " + e.getMessage());
        }

        return false;
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }

    private void closeSocket() {
        try {
            clientSocket.close();
        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao fechar socket: " + e.getMessage());
        }
    }
}
