package client;

import server.models.Response;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientRouter {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public ClientRouter(DataInputStream in, DataOutputStream out, Socket socket) {
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    public void handleResponses() {
        try {
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
