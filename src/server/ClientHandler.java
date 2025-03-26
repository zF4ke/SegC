package server;

import server.models.*;
import server.utils.InputUtils;
import server.utils.NetworkUtils;

import java.io.*;
import java.net.Socket;
import java.util.List;

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

    /**
     * Open the input and output streams.
     */
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
            Request request = Request.fromStream(in);
            Body body = request.getBody();

            if (body.getFormat() != BodyFormat.JSON) {
                this.createErrorAuthResponse(StatusCode.BAD_REQUEST);
                return false;
            }

            System.out.println("Request: " + request);

            BodyJSON json = request.getBodyJSON();
            String userId = json.get("userId");
            String password = json.get("password");

            if (!InputUtils.isValidUsernameAndPassword(userId, password)) {
                this.createErrorAuthResponse(StatusCode.BAD_REQUEST);
                return false;
            }

            //System.out.println(request);
            System.out.println("[SERVER] Autenticar cliente: " + userId);

            StatusCode status = authManager.authenticate(userId, password);
            List<StatusCode> OK_CODES = List.of(StatusCode.OK_USER, StatusCode.OK_NEW_USER);

            if (OK_CODES.contains(status)) {
                this.authenticatedUser = authManager.getUser(userId);
                Response response = new Response(
                        request.getUUID(),
                        status,
                        BodyFormat.JSON,
                        new BodyJSON()
                );
                //System.out.println(response);
                this.out.write(response.toByteArray());

                return true;
            } else if (status == StatusCode.WRONG_PWD) {
                Response response = new Response(
                        request.getUUID(),
                        status,
                        BodyFormat.JSON,
                        new BodyJSON()
                );
                this.out.write(response.toByteArray());

                // allow the client to retry once
                Request retryRequest = Request.fromStream(in);
                Body retryBody = retryRequest.getBody();

                System.out.println("[SERVER] Resposta Segunda Tentativa: " + retryRequest);

                if (retryBody.getFormat() != BodyFormat.JSON) {
                    this.createErrorAuthResponse(StatusCode.BAD_REQUEST);
                    return false;
                }

                BodyJSON retryJson = retryRequest.getBodyJSON();
                String retryPassword = retryJson.get("password");

                status = authManager.authenticate(userId, retryPassword);

                if (OK_CODES.contains(status)) {
                    this.authenticatedUser = authManager.getUser(userId);
                    response = new Response(
                            retryRequest.getUUID(),
                            status,
                            BodyFormat.JSON,
                            new BodyJSON()
                    );
                    this.out.write(response.toByteArray());

                    return true;
                }
            }

            this.createErrorAuthResponse(status);
            return false;
        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao autenticar cliente: " + e.getMessage());
        }

        return false;
    }

    private void createErrorAuthResponse(StatusCode status) {
        try {
            Response response = new Response(
                    NetworkUtils.randomUUID(),
                    status,
                    BodyFormat.JSON,
                    new BodyJSON()
            );
            this.out.write(response.toByteArray());
        } catch (IOException e) {
            System.err.println("[SERVER] Erro ao criar resposta de autenticação: " + e.getMessage());
        }
    }

    private void closeSocket() {
        try {
            clientSocket.close();
        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao fechar socket: " + e.getMessage());
        }
    }
}
