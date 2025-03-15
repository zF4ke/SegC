package client;

import server.models.Response;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientRouter {
    private final Socket socket;

    public ClientRouter(Socket clientSocket) {
        this.socket = clientSocket;
    }

    public void handleResponses() {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            Response response = Response.fromStream(in);
            System.out.println("[CLIENT] Resposta recebida: " + response);

            System.out.println(response.getBody().toString());
            System.out.println(response.getFormat());
            System.out.println(response.getUUID());
            System.out.println(response.getSize());
            System.out.println(response.getStatus());

        } catch (Exception e) {
            System.err.println("[CLIENT] Erro ao processar resposta: " + e.getMessage());
        }
    }

    /**
     * Close the socket.
     */
    private void closeSocket() {
        try {
            socket.close();
        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao fechar socket: " + e.getMessage());
        }
    }
}
