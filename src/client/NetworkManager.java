package client;

import server.models.*;
import server.utils.NetworkUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NetworkManager {
    private final DataInputStream in;
    private final DataOutputStream out;

    public NetworkManager(DataInputStream in, DataOutputStream out) {
        this.in = in;
        this.out = out;
    }

    public void createWorkspace(String workspaceName) {
        try {
            BodyJSON body = new BodyJSON();
            body.put("workspaceName", workspaceName);

            Request request = new Request(
                    NetworkUtils.randomUUID(),
                    BodyFormat.JSON,
                    "createWorkspace",
                    body
            );

            out.write(request.toByteArray());

            Response response = Response.fromStream(in);
            System.out.println("[CLIENT] Resposta recebida do servidor: " + response.getStatus());

        } catch (IOException e) {
            System.err.println("[CLIENT] Erro ao criar workspace: " + e.getMessage());
        }
    }
}