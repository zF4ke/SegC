package client;

import server.utils.InputUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CommandLineInterface {
    private final NetworkManager networkManager;
    private final Scanner scanner;
    private final Socket socket;

    /**
     * Create a new command line interface.
     *
     * @param in the input stream
     * @param out the output stream
     */
    public CommandLineInterface(Socket socket, DataInputStream in, DataOutputStream out) {
        this.networkManager = new NetworkManager(in, out);
        this.scanner = new Scanner(System.in);
        this.socket = socket;

        // Shutdown hook to close gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[CLIENT] Shutting down...");
            scanner.close();
        }));
    }

    /**
     * Start the command line interface.
     */
    public void start() {
        while (true) {
            System.out.println("\n[CLIENT] Comandos disponiveis para uso:");
            System.out.println("[CLIENT] CREATE <ws> # Criar um novo workspace - utilizador é Owner.");
            System.out.println("[CLIENT] ADD <user1> <ws> # Adicionar utilizador <user1> ao workspace <ws>. " +
                    "A operação ADD só funciona se o utilizador for o Owner do workspace <ws>");
            System.out.println("[CLIENT] UP <ws> <file1> ... <filen> # Adicionar ficheiros ao workspace.");
            System.out.println("[CLIENT] DW <ws> <file1> ... <filen> # Download de ficheiros do workspace para a maquina local.");
            System.out.println("[CLIENT] RM <ws> <file1> ... <filen> # Apagar ficheiros do workspace.");
            System.out.println("[CLIENT] LW # Lista os workspaces associados ao utilizador.");
            System.out.println("[CLIENT] LS <ws> # Lista os ficheiros dentro de um workspace.\n");

            System.out.print("Comando: ");
            String input = scanner.nextLine();
            String[] commands = input.split("\n");

            for (String command : commands) {
                String[] commandParts = command.split(" ");
                String commandAction = commandParts[0].toUpperCase();
                if(!InputUtils.isAlfaNumeric(commandAction)) {
                    System.err.println("[CLIENT] Comando invalido: " + commandAction);
                    continue;
                }

                switch (commandAction) {
                    case "CREATE":
                        if (commandParts.length == 2) {
                            String workspace = commandParts[1];
                            if (!isValidWorkspace(workspace)) {break;}

                            networkManager.createWorkspace(workspace);
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: CREATE");
                        }
                        break;
                    case "ADD":
                        if (commandParts.length == 3) {
                            String user = commandParts[1];
                            String workspace = commandParts[2];
                            if(!isValidUser(user) || !isValidWorkspace(workspace)) {break;}

                            networkManager.addUserToWorkspace(user, workspace);
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: ADD");
                        }
                        break;
                    case "UP":
                        if (commandParts.length >= 3) {
                            String workspace = commandParts[1];
                            if (!isValidWorkspace(workspace)) {break;}

                            String[] files = new String[commandParts.length - 2];
                            System.arraycopy(commandParts, 2, files, 0, commandParts.length - 2);

                            String[] validFiles = validFiles(files);
                            if (validFiles.length == 0) {break;}

                            networkManager.uploadFilesToWorkspace(workspace, files);
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: UP");
                        }
                        break;
                    case "DW":
                        if (commandParts.length >= 3) {
                            String workspace = commandParts[1];
                            if (!isValidWorkspace(workspace)) {break;}

                            String[] files = new String[commandParts.length - 2];
                            System.arraycopy(commandParts, 2, files, 0, commandParts.length - 2);

                            String[] validFiles = validFiles(files);
                            if (validFiles.length == 0) {break;}

                            networkManager.downloadFilesFromWorkspace(workspace, files);
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: DW");
                        }
                        break;
                    case "RM":
                        if (commandParts.length >= 3) {
                            String workspace = commandParts[1];
                            if (!isValidWorkspace(workspace)) {break;}

                            String[] files = new String[commandParts.length - 2];
                            System.arraycopy(commandParts, 2, files, 0, commandParts.length - 2);

                            String[] validFiles = validFiles(files);
                            if (validFiles.length == 0) {break;}

                            networkManager.removeFilesFromWorkspace(workspace, files);
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: RM");
                        }
                        break;
                    case "LW":
                        if (commandParts.length == 1) {
                            networkManager.listWorkspaces();
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: LW");
                        }
                        break;
                    case "LS":
                        if (commandParts.length == 2) {
                            String workspace = commandParts[1];
                            if (!isValidWorkspace(workspace)) {break;}

                            networkManager.listFilesWorkspace(workspace);
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: LS");
                        }
                        break;
                    default:
                        System.err.println("[CLIENT] Comando invalido: " + commandAction);
                }
            }
        }
    }

    /**
     * Check if the workspace is valid.
     *
     * @param workspace the workspace
     * @return true if the workspace is valid, false otherwise
     */
    private boolean isValidWorkspace(String workspace) {
        if(!InputUtils.isValidWorkspaceId(workspace)) {
            System.err.println("[CLIENT] Nome de workspace invalido!");
            return false;
        }

        return true;
    }

    /**
     * Check if the user is valid.
     *
     * @param user the user
     * @return true if the user is valid, false otherwise
     */
    private boolean isValidUser(String user) {
        if(!InputUtils.isValidUserId(user)) {
            System.err.println("[CLIENT] Nome de utilizador invalido!");
            return false;
        }

        return true;
    }

    /**
     * Return the valid files.
     *
     * @param files the files to be checked
     * @return the valid files
     */
    private String[] validFiles(String[] files) {
    List<String> validFilesList = new ArrayList<>();
    int filesToUpload = files.length;
    for (String file : files) {
        if (!InputUtils.isValidFilename(file)) {
            System.err.println("[CLIENT] Nome de ficheiro invalido: " + file);
            //System.err.println("[CLIENT] Ficheiro nao enviado para o servidor.");
            filesToUpload--;
        } else {
            validFilesList.add(file);
        }
    }
    return validFilesList.toArray(new String[filesToUpload]);
}
}
