package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The server class.
 */
public class MySharingServer {
    private static final int DEFAULT_PORT = 12345;
    private final ServerSocket serverSocket;

    public static void main(String[] args) {
        int port = parsePortArgs(args);

        try {
            MySharingServer server = new MySharingServer(port);
            server.start();
        } catch (IOException e) {
            System.err.println("[SERVER] Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    /**
     * Parse the port argument from the command line.
     *
     * @param args the command line arguments
     * @return the port number
     */
    private static int parsePortArgs(String[] args) {
        if (args.length == 0) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("[SERVER] Porta inválida: " + args[0]);
            System.exit(1);
            return -1;
        }
    }

    /**
     * Create a new server instance.
     *
     * @param port the port number
     * @throws IOException if an I/O error occurs
     */
    public MySharingServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        System.out.println("[SERVER] Servidor iniciado na porta " + port);
    }

    /**
     * Start the server.
     */
    public void start() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Erro ao aceitar conexão: " + e.getMessage());
        } finally {
            stop();
        }
    }

    /**
     * Stop the server.
     */
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("[SERVER] Servidor fechado");
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Erro ao fechar o servidor: " + e.getMessage());
        }
    }
}
