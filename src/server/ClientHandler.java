package server;

import server.models.User;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket clientSocket;

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
        System.out.println("[SERVER] Cliente conectado: " + clientSocket.getInetAddress().getHostAddress());

        if (!authenticateClient()) {
            System.out.println("[SERVER] Autenticação falhou. A fechar a ligação.");
            return;
        }

        System.out.println("[SERVER] Autenticação bem sucedida. A aguardar por comandos...");
    }

    /**
     * Authenticate the client.
     *
     * @return true if the client is authenticated, false otherwise
     */
    private boolean authenticateClient() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String credentials = in.readLine();
            if (credentials == null || !credentials.contains(" ")) {
                out.println(ServerResponse.INVALID_AUTH);
                return false;
            }

            String[] parts = credentials.split(" ");
            String userId = parts[0];
            String password = parts[1];

            ServerResponse response = authManager.authenticate(userId, password);
            out.println(response);

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
}
