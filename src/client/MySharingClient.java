package client;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import server.models.*;
import server.utils.InputUtils;
import server.utils.NetworkUtils;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * The client class.
 */
public class MySharingClient {
    private static final int DEFAULT_PORT = 12345;
    private final String serverAddress;
    private final int port;
    private final String userId;
    private final String password;

    private SSLSocket sslSocket;
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
            // Configurar o truststore (certificados confiáveis)
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
            System.setProperty("javax.net.ssl.trustStore", "client_keys/" + userId + "/" + userId + ".truststore");
            System.setProperty("javax.net.ssl.trustStorePassword", "123456");
            SocketFactory sf = SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket) sf.createSocket(serverAddress, port);
            this.openStreams();

            System.out.println("[CLIENT] Conectado ao servidor " + serverAddress + ":" + port);

            List<StatusCode> OK_CODES = List.of(StatusCode.OK_USER, StatusCode.OK_NEW_USER);

            StatusCode statusCode = authenticateUser(); 
            if (!OK_CODES.contains(statusCode)) {
                System.err.println("[CLIENT] " + statusCode + "\n[CLIENT] Autenticação falhou.");
                stop();
                return;
            }

            System.out.println("[CLIENT] " + statusCode + "\n[CLIENT] Autenticação bem sucedida.");

            CommandLineInterface cli = new CommandLineInterface(sslSocket, in, out, userId);
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
            this.in = new DataInputStream(sslSocket.getInputStream());
            this.out = new DataOutputStream(sslSocket.getOutputStream());

            System.out.println("[CLIENT] Streams abertas.");
        } catch (IOException e) {
            System.err.println("[CLIENT] Erro ao abrir streams: " + e.getMessage());
        }
    }

    /**
     * Authenticate the user.
     *
     * @return true if the user is authenticated, false otherwise
     */
    public StatusCode authenticateUser() {
        try {
            System.out.println("[CLIENT] Autenticando utilizador...");

            if (!InputUtils.isValidUsernameAndPassword(userId, password)) {
                System.err.println("[CLIENT] Utilizador ou password tem caracteres inválidos.");
                return StatusCode.BAD_REQUEST;
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

            //System.out.println("[CLIENT] Resposta: " + response);

            List<StatusCode> OK_CODES = List.of(StatusCode.OK_USER, StatusCode.OK_NEW_USER);

            if (OK_CODES.contains(response.getStatus())) {
                return response.getStatus();
            } else if (response.getStatus() == StatusCode.WRONG_PWD) {
                
                Scanner s = new Scanner(System.in);

                System.out.print("[CLIENT] Password incorreta. Tente novamente.\n[CLIENT] Segunda tentativa: ");
                String newPassword = s.nextLine();

                body.put("password", newPassword);
                request = new Request(
                        NetworkUtils.randomUUID(),
                        BodyFormat.JSON,
                        "authenticate",
                        body
                );

                this.out.write(request.toByteArray());
                response = Response.fromStream(in);

                //System.out.println("[CLIENT] Resposta Segunda Tentativa: " + response);

                return response.getStatus();
            }
        } catch (Exception e) {
            System.err.println("[CLIENT] Erro ao autenticar utilizador: " + e.getMessage());
        }

        return StatusCode.NOK;
    }

    /**
     * Stop the client.
     */
    public void stop() {
        try {
            if (sslSocket != null && !sslSocket.isClosed()) {
                sslSocket.close();
                System.out.println("[CLIENT] Conexão fechada");
            }
        } catch (Exception e) {
            System.err.println("[CLIENT] Erro ao fechar a conexão: " + e.getMessage());
        }
    }
}
