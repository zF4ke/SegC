package client;

import server.models.*;
import server.utils.NetworkUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NetworkManager {
    private final DataInputStream in;
    private final DataOutputStream out;

    /**
     * Create a new network manager.
     *
     * @param in the input stream
     * @param out the output stream
     */
    public NetworkManager(DataInputStream in, DataOutputStream out) {
        this.in = in;
        this.out = out;
    }

    /**
     * Sends a request to the server to create a workspace.
     *
     * @param workspaceName the workspace name
     */
    public void createWorkspace(String workspaceName) {
        BodyJSON body = new BodyJSON();
        body.put("workspaceName", workspaceName);

        buildRequest(body, "createWorkspace");
    }

    /**
     * Sends a request to the server to add a user to a workspace.
     *
     * @param user the user
     * @param workspace the workspace
     */
    public void addUserToWorkspace(String user, String workspace) {
        BodyJSON body = new BodyJSON();
        body.put("user", user);
        body.put("workspace", workspace);

        buildRequest(body, "addUserToWorkspace");
    }

    /**
     * Sends a request to the server to upload files to a workspace.
     *
     * @param workspace the workspace
     * @param files the files
     */
    public void uploadFilesToWorkspace(String workspace, String[] files) {
        BodyJSON body = new BodyJSON();
        body.put("workspace", workspace);

        String filesString = String.join(",", files); // Use split to get the files back
        body.put("files", filesString);

        buildRequest(body, "uploadFilesToWorkspace");
    }

    /**
     * Sends a request to the server to download files from a workspace.
     *
     * @param workspace the workspace
     * @param files the files
     */
    public void downloadFilesToWorkspace(String workspace, String[] files) {
        BodyJSON body = new BodyJSON();
        body.put("workspace", workspace);

        String filesString = String.join(",", files);
        body.put("files", filesString);

        buildRequest(body, "downloadFilesToWorkspace");
    }

    /**
     * Sends a request to the server to remove files from a workspace.
     *
     * @param workspace the workspace
     * @param files the files
     */
    public void removeFilesFromWorkspace(String workspace, String[] files) {
        BodyJSON body = new BodyJSON();
        body.put("workspace", workspace);

        String filesString = String.join(",", files);
        body.put("files", filesString);

        buildRequest(body, "removeFilesFromWorkspace");
    }

    /**
     * Sends a request to the server to list workspaces.
     */
    public void listWorkspaces() {
        BodyJSON body = new BodyJSON();

        buildRequest(body, "listWorkspaces");
    }

    /**
     * Sends a request to the server to list the files in a workspace.
     *
     * @param workspace the workspace
     */
    public void listFilesWorkspace(String workspace) {
        BodyJSON body = new BodyJSON();
        body.put("workspace", workspace);

        buildRequest(body, "listFilesWorkspace");
    }

    /**
     * Sends a request to the server and waits for a response.
     *
     * @param body the request body
     * @param route the route
     */
    private void buildRequest(BodyJSON body, String route) {
        try {
            Request request = new Request(
                    NetworkUtils.randomUUID(),
                    BodyFormat.JSON,
                    route,
                    body
            );

            out.write(request.toByteArray());

            Response response = Response.fromStream(in);
            System.out.println("[CLIENT] Resposta recebida do servidor: " + response.getStatus());

        } catch (IOException e) {
            System.err.println("[CLIENT] Erro ao processar pedido: " + e.getMessage());
        }
    }
}