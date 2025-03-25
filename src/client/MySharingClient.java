package client;

import server.models.*;
import server.utils.InputUtils;
import server.utils.NetworkUtils;

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
    private DataInputStream in;
    private DataOutputStream out;

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
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            System.out.println("[CLIENT] Conectado ao servidor " + serverAddress + ":" + port);

            if (!authenticateUser()) {
                System.err.println("[CLIENT] Autenticação falhou.");
                stop();
                return;
            }

            System.out.println("[CLIENT] Autenticação bem sucedida.");

            CommandLineInterface cli = new CommandLineInterface(in, out);
            cli.start();

        } catch (Exception e) {
            System.err.println("[CLIENT] Erro ao conectar ao servidor: " + e.getMessage());
        }
    }

    /**
     * Open the input and output streams.
     */
    private void openStreams() {
        try {
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            System.out.println("[SERVER] Streams abertas.");
        } catch (IOException e) {
            System.err.println("[SERVER] Erro ao abrir streams: " + e.getMessage());
        }
    }

    /**
     * Authenticate the user.
     *
     * @return true if the user is authenticated, false otherwise
     */
    public boolean authenticateUser() {
        try {
            System.out.println("[CLIENT] Autenticando utilizador...");

            if (!InputUtils.isValidUsernameAndPassword(userId, password)) {
                System.err.println("[CLIENT] Utilizador ou password tem caracteres inválidos.");
                return false;
            }

            BodyJSON body = new BodyJSON();
            body.put("userId", userId);
            body.put("password", password);

            Request request = new Request(
                    NetworkUtils.randomUUID(),
                    BodyFormat.JSON,
                    "authenticate",
                    body
            );

            this.out.write(request.toByteArray());

            Response response = Response.fromStream(in);
            return response.getStatus() == StatusCodes.OK;

        } catch (Exception e) {
            System.err.println("[CLIENT] Erro ao autenticar utilizador: " + e.getMessage());
        }

        return false;
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
