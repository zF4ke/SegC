package server;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import server.utils.SecurityUtils;

/**
 * The server class.
 */
public class MySharingServer {
    private static final int DEFAULT_PORT = 12345;
    private static SecretKey serverKey;
    private static boolean verifyUsersMacFlag = true;
    private static boolean verifyWorkspacesMacFlag = true;
    private static final Path USERS_FILE_PATH = Path.of("data/users.txt");
    private static final Path USERS_MAC_FILE_PATH = Path.of("data/users.mac");
    private static final Path WORKSPACES_FILE_PATH = Path.of("data/workspaces.txt");
    private static final Path WORKSPACES_MAC_FILE_PATH = Path.of("data/workspaces.mac");
    private final ServerSocket serverSocket;

    public static void main(String[] args) {
        int port = parsePortArgs(args);

        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("[SERVER] Bem-vindo ao MySharingServer!");
            System.out.print("[SERVER] Introduza a password do sistema: ");
            String password = scanner.nextLine();

            // Pass sem salt
            // Verificar se deviamos utilizar assim como o prof tem no mail ou se
            // devemos utilizar o salt e o genSecretKey para ser mais seguro
            byte[] bytesKey = password.getBytes();
            serverKey = new SecretKeySpec(bytesKey, 0, bytesKey.length, "AES");

            verifyFilesIntegrity();
            scanner.close();

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
     * Verify the integrity of the files.
     */
    private static void verifyFilesIntegrity() {
        try {
            if (!SecurityUtils.verifyFileMac(USERS_FILE_PATH, USERS_MAC_FILE_PATH, serverKey)) {
                handleMacIssue(USERS_FILE_PATH, USERS_MAC_FILE_PATH, "users");
            }
            if (!SecurityUtils.verifyFileMac(WORKSPACES_FILE_PATH, WORKSPACES_MAC_FILE_PATH, serverKey)) {
                handleMacIssue(WORKSPACES_FILE_PATH, WORKSPACES_MAC_FILE_PATH, "workspaces");
            }
        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao verificar a integridade dos ficheiros: " + e.getMessage());
            System.err.println("[SERVER] Sistema comprometido! A encerrar...");
            System.exit(1);
        }
    }

    /**
     * Verify the MAC of the users file.
     */
    public static void verifyUsersMac() {
        if (!verifyUsersMacFlag) {
            return;
        }
        try {
            if (!SecurityUtils.verifyFileMac(USERS_FILE_PATH, USERS_MAC_FILE_PATH, serverKey)) {
                System.err.println("[SERVER] MAC inválido. O arquivo de usuários pode ter sido comprometido.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao verificar MAC do arquivo Users: " + e.getMessage());
        }
    }

    /**
     * Update the MAC of the users file.
     */
    public static void updateUsersMac() {
        if (!verifyUsersMacFlag) {
            return;
        }
        try {
            byte[] mac = SecurityUtils.genFileMac(USERS_FILE_PATH, serverKey);
            SecurityUtils.writeMacOnMacFile(USERS_MAC_FILE_PATH, mac);
        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao atualizar MAC do arquivo de Users: " + e.getMessage());
        }
    }
    
    /**
     * Verify the MAC of the workspaces file.
     */
    public static void verifyWorkspacesMac() {
        if (!verifyWorkspacesMacFlag) {
            return;
        }
        try {
            if (!SecurityUtils.verifyFileMac(WORKSPACES_FILE_PATH, WORKSPACES_MAC_FILE_PATH, serverKey)) {
                System.err.println("[SERVER] MAC inválido. O arquivo de workspaces pode ter sido comprometido.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao verificar MAC do arquivo Workspaces: " + e.getMessage());
        }
    }

    /**
     * Update the MAC of the workspaces file.
     */
    public static void updateWorkspacesMac() {
        if (!verifyWorkspacesMacFlag) {
            return;
        }
        try {
            byte[] mac = SecurityUtils.genFileMac(WORKSPACES_FILE_PATH, serverKey);
            SecurityUtils.writeMacOnMacFile(WORKSPACES_MAC_FILE_PATH, mac);
        } catch (Exception e) {
            System.err.println("[SERVER] Erro ao atualizar MAC do arquivo de Workspaces: " + e.getMessage());
        }
    }

    /**
     * Handle the MAC issue for a file.
     *
     * @param filePath    the file path to handle
     * @param macFilePath the MAC file path of that file
     * @throws Exception if an error occurs
     */
    private static void handleMacIssue(Path filePath, Path macFilePath, String macVerificationFlag) throws Exception {
        if (!Files.exists(filePath)) {
            System.out.println("[SERVER] Ficheiro ausente: " + filePath);
            System.out.println("[SERVER] A encerrar ...");
            System.exit(1);
        } else if (!Files.exists(macFilePath)) {
            System.out.println("[SERVER] MAC ausente para o ficheiro: " + filePath);
            System.out.print("[SERVER] Deseja calcular o MAC? (s/n): ");
            Scanner scanner = new Scanner(System.in); // Nao fechar este scanner
            String answer = scanner.nextLine();
            if (answer.equalsIgnoreCase("s")) {
                byte[] mac = SecurityUtils.genFileMac(filePath, serverKey);
                SecurityUtils.writeMacOnMacFile(macFilePath, mac);
                System.out.println("[SERVER] MAC calculado e armazenado com sucesso.");
            } else {
                if (macVerificationFlag.equals("users")) {
                    verifyUsersMacFlag = false;
                } else if (macVerificationFlag.equals("workspaces")) {
                    verifyWorkspacesMacFlag = false;
                }
                System.out.println("[SERVER] Prosseguindo sem criacao de MAC...");
            }
        } else {
            System.err.println("[SERVER] MAC inválido para o ficheiro: " + filePath);
            System.err.println("[SERVER] Sistema comprometido! A encerrar...");
            System.exit(1);
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
