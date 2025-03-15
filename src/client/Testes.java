package client;

import server.models.*;
import server.utils.NetworkUtils;

import java.io.*;

public class Testes {


    // APAGAR DEPOIS
    public static void tests(DataInputStream in, DataOutputStream out) throws IOException {
        /* ============================================================================= */
        /* TESTE DE ENVIO DE REQUEST JSON */
        /* ============================================================================= */
        testHeader("Teste de envio de request JSON");

        BodyJSON body = new BodyJSON();
        body.put("username", "user1");
        body.put("password", "pass1");

        Request request = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "example",
                body
        );
        request.addHeader("REPORTE", "OPCIONAL");

        System.out.println("[CLIENT] A enviar request: " + request);
        out.write(request.toByteArray());

        Response response = Response.fromStream(in);
        System.out.println(response);

        // Verificações
        testSeparator();

        assertPrint(StatusCodes.OK, response.getStatus());
        assertPrint(request.getUUID(), response.getUUID());
        assertPrint(BodyFormat.JSON, response.getFormat());
        assertPrint(true, response.getBody() instanceof BodyJSON);
        assertPrint(true, ((BodyJSON) response.getBody()).containsKey("message"));
        assertPrint("logado com sucesso!", ((BodyJSON) response.getBody()).get("message"));

        waitForAssert();
        testFooter();
        /* ============================================================================= */
        /* FIM DO TESTE JSON */
        /* ============================================================================= */




        /* ============================================================================= */
        /* TESTE DE ENVIO DE REQUEST RAW */
        /* ============================================================================= */
        testHeader("Teste de envio de request RAW");

        BodyRaw braw = new BodyRaw(new byte[] { 0x01, 0x02, 0x03, 0x05 });

        Request request2 = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.RAW,
                "b",
                braw
        );
        request2.addHeader("REPORTE", "OBRIGATORIO");

        System.out.println("[CLIENT] A enviar request: " + request2);

        out.write(request2.toByteArray());

        Response response2 = Response.fromStream(in);
        System.out.println(response2); // É suposto dar erro, porque não existe rota "b", mas os dados devem ser enviados corretamente

        // Verificações
        testSeparator();

        assertPrint(StatusCodes.NOT_FOUND, response2.getStatus());
        assertPrint(request2.getUUID(), response2.getUUID());
        assertPrint(BodyFormat.RAW, request2.getFormat());
        assertPrint(BodyFormat.JSON, response2.getFormat());
        assertPrint(true, response2.getBody() instanceof BodyJSON);
        assertPrint(true, ((BodyJSON) response2.getBody()).containsKey("error"));
        assertPrint("Rota não encontrada", ((BodyJSON) response2.getBody()).get("error"));

        waitForAssert();
        testFooter();
        /* ============================================================================= */
        /* FIM DO TESTE RAW */
        /* ============================================================================= */




        /* ============================================================================= */
        /* TESTE DE ENVIO DE FICHEIRO BINÁRIO TXT PEQUENO (file.txt) */
        /* ============================================================================= */
        testHeader("Teste de envio de ficheiro binário pequeno");

        File file = new File("file.txt");

        // Send file.txt to the server using the unified fileTransfer route
        String tempFileId = sendFileToServer(file.getPath(), in, out);
        System.out.println("[CLIENT] File uploaded with temporary ID: " + tempFileId);

        // Now use the workspace upload route that will process the temporary file
//        BodyJSON workspaceRequest = new BodyJSON();
//        workspaceRequest.put("action", "attachFile");
//        workspaceRequest.put("workspaceId", "workspace123");
//        workspaceRequest.put("tempFileId", tempFileId);
//        workspaceRequest.put("fileName", file.getName());
//
//        Request request3 = new Request(
//                NetworkUtils.randomUUID(),
//                BodyFormat.JSON,
//                "uploadWorkspace",
//                workspaceRequest
//        );
//
//        System.out.println("[CLIENT] Associating file to workspace: " + request3);
//        out.write(request3.toByteArray());
//
//        Response response3 = Response.fromStream(in);
//        System.out.println(response3);

        // Verificações

        waitForAssert();
        testFooter();
        /* ============================================================================= */
//
//
//
//        /* ============================================================================= */
//        /* TESTE DE ENVIO DE FICHEIRO BINÁRIO MEDIO (spongepls.mp3) */
//        /* ============================================================================= */
        testHeader("Teste de envio de ficheiro binário médio");

        File file2 = new File("spongepls.mp3");

        // Send spongepls.mp3 to the server using the unified fileTransfer route
        String tempFileId2 = sendFileToServer(file2.getPath(), in, out);
        System.out.println("[CLIENT] File uploaded with temporary ID: " + tempFileId2);
//
//        // Now use the workspace upload route that will process the temporary file
//        BodyJSON workspaceRequest2 = new BodyJSON();
//        workspaceRequest2.put("action", "attachFile");
//        workspaceRequest2.put("workspaceId", "workspace123");
//        workspaceRequest2.put("tempFileId", tempFileId2);
//        workspaceRequest2.put("fileName", file2.getName());
//
//        Request request4 = new Request(
//                NetworkUtils.randomUUID(),
//                BodyFormat.JSON,
//                "uploadWorkspace",
//                workspaceRequest2
//        );
//
//        System.out.println("[CLIENT] Associating file to workspace: " + request4);
//        out.write(request4.toByteArray());
//
//        Response response4 = Response.fromStream(in);
//        System.out.println(response4);
//
        // Verificações

        waitForAssert();
        testFooter();
        /* ============================================================================= */

    }

    private static String sendFileToServer(String filePath, DataInputStream in, DataOutputStream out) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("[CLIENT] Ficheiro não encontrado: " + filePath);
            return null;
        }

        // Step 1: Initialize the upload
        System.out.println("[CLIENT] Iniciando envio do ficheiro: " + file.getName());

        BodyJSON initBody = new BodyJSON();
        initBody.put("action", "init");
        initBody.put("fileName", file.getName());
        initBody.put("size", String.valueOf(file.length()));

        int chunkSize = 1024 * 64; // 64KB chunks
        int totalChunks = (int) Math.ceil((double) file.length() / chunkSize);
        initBody.put("chunks", String.valueOf(totalChunks));

        Request initRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "fileupload",
                initBody
        );

        out.write(initRequest.toByteArray());
        Response initResponse = Response.fromStream(in);
        System.out.println("[CLIENT] Resposta de inicialização: " + initResponse);

        if (initResponse.getStatus() != StatusCodes.OK) {
            System.err.println("[CLIENT] Erro ao inicializar upload");
            return null;
        }

        String fileId = ((BodyJSON) initResponse.getBody()).get("fileId");

        // Step 2: Send file chunks
        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[chunkSize];
            int chunkId = 0;
            int bytesRead;

            while ((bytesRead = fileIn.read(buffer)) > 0) {
                byte[] chunkData;
                if (bytesRead < buffer.length) {
                    chunkData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                } else {
                    chunkData = buffer;
                }

                BodyRaw chunkBody = new BodyRaw(chunkData);
                Request chunkRequest = new Request(
                        NetworkUtils.randomUUID(),
                        BodyFormat.RAW,
                        "fileupload",
                        chunkBody
                );
                chunkRequest.addHeader("FILE-ID", fileId);
                chunkRequest.addHeader("CHUNK-ID", String.valueOf(chunkId));
                chunkRequest.addHeader("TYPE", "CHUNK");

                System.out.println("[CLIENT] Enviando chunk " + (chunkId + 1) + "/" + (totalChunks));
                out.write(chunkRequest.toByteArray());

                Response chunkResponse = Response.fromStream(in);
                if (chunkResponse.getStatus() != StatusCodes.OK) {
                    System.err.println("[CLIENT] Erro ao enviar chunk " + chunkId);
                    return null;
                }

                chunkId++;
            }
        }

        // Step 3: Complete the upload
        BodyJSON completeBody = new BodyJSON();
        completeBody.put("action", "complete");
        completeBody.put("fileId", fileId);

        Request completeRequest = new Request(
                NetworkUtils.randomUUID(),
                BodyFormat.JSON,
                "fileupload",
                completeBody
        );

        out.write(completeRequest.toByteArray());
        Response completeResponse = Response.fromStream(in);

        if (completeResponse.getStatus() != StatusCodes.OK) {
            System.err.println("[CLIENT] Erro ao finalizar upload");
            return null;
        }

        System.out.println("[CLIENT] Ficheiro enviado com sucesso!");
        return fileId;
    }

    private static void testHeader(String name) {
        System.out.println(
                "=============================================================================\n" +
                "     TESTE: " + name + "\n" +
                "============================================================================="
        );
    }

    private static void testSeparator() {
        System.out.println(
                "-----------------------------------------------------------------------------"
        );
    }

    private static void testFooter() {
        System.out.println(
                "=============================================================================\n"
        );
    }

    private static boolean assertPrint(Object expected, Object actual) {
        if (expected.equals(actual)) {
            System.out.println("[TEST] OK");
            return true;
        } else {
            System.err.println("[TEST] ERRO: Esperado: " + expected + ", obtido: " + actual);
            return false;
        }
    }

    private static void waitForAssert() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
