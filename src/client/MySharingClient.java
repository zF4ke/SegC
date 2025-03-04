package client;

import server.ServerResponse;

import java.io.*;
import java.net.Socket;

/**
 * The client class.
 */
public class MySharingClient {
    private static final int DEFAULT_PORT = 12345;
    private final String serverAddress;
    private final int port;
    private final String userId;
    private final String password;

    private Socket socket;

    public static void main(String[] args) {
        MySharingClient client = parseArgs(args);
        client.start();
    }

    /**
     * Parse the command line arguments.
     *
     * @param args the command line arguments
     * @return the client instance
     */
    private static MySharingClient parseArgs(String[] args) {
        if (args.length < 3) {
            System.err.println("[CLIENT] Argumentos insuficientes: mySharingClient <serverAddress> <user-id> <password>");
            System.exit(1);
        }

        String serverAddress = args[0];
        String userId = args[1];
        String password = args[2];

        int port = DEFAULT_PORT;
        if (serverAddress.contains(":")) {
            String[] parts = serverAddress.split(":");
            serverAddress = parts[0];
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                System.err.println("[CLIENT] Porta inválida: " + parts[1]);
                System.exit(1);
            }
        }

        return new MySharingClient(serverAddress, port, userId, password);
    }

    /**
     * Create a new client instance.
     *
     * @param serverAddress the server address
     * @param port          the port number
     * @param userId        the user ID
     * @param password      the password
     */
    public MySharingClient(String serverAddress, int port, String userId, String password) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.userId = userId;
        this.password = password;
    }

    /**
     * Start the client.
     */
    public void start() {
        try {
            socket = new Socket(serverAddress, port);

            System.out.println("[CLIENT] Conectado ao servidor " + serverAddress + ":" + port);

            if (authenticateUser() != ServerResponse.OK) {
                System.err.println("[CLIENT] Autenticação falhou.");
                stop();
            }

            System.out.println("[CLIENT] Autenticação bem sucedida.");

        } catch (Exception e) {
            System.err.println("[CLIENT] Erro ao conectar ao servidor: " + e.getMessage());
        }
    }

    public ServerResponse authenticateUser() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(userId + " " + password);

            System.out.println("[CLIENT] Utilizador: \"" + userId + "\" e password: \"" + password + "\" enviados.");

            String response = in.readLine();
            ServerResponse serverResponse = ServerResponse.valueOf(response);

            System.out.println("[CLIENT] Autenticação: " + serverResponse);

            return serverResponse;
        } catch (Exception e) {
            System.err.println("[CLIENT] Erro ao autenticar utilizador: " + e.getMessage());
        }

        return ServerResponse.ERROR;
    }

    /**
     * Stop the client.
     */
    public void stop() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("[CLIENT] Conexão fechada");
            }
        } catch (Exception e) {
            System.err.println("[CLIENT] Erro ao fechar a conexão: " + e.getMessage());
        }
    }
}
