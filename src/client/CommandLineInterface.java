package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Scanner;

import static server.utils.InputUtils.isAlfaNumeric;

public class CommandLineInterface {
    private final NetworkManager networkManager;
    private final Scanner scanner;

    /**
     * Create a new command line interface.
     *
     */
    public CommandLineInterface(DataInputStream in, DataOutputStream out) {
        this.networkManager = new NetworkManager(in, out);
        this.scanner = new Scanner(System.in);

        // Shutdown hook to close gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[CLIENT] Shutting down...");
            scanner.close();
        }));
    }

    /**
     * Start the command line interface.
     */
    public void start() {
        while (true) {
            System.out.println("[CLIENT] Comandos disponiveis para uso:");
            System.out.println("[CLIENT] CREATE <ws> # Criar um novo workspace - utilizador `e Owner.");
            System.out.println("[CLIENT] ADD <user1> <ws> # Adicionar utilizador <user1> ao workspace <ws>. " +
                    "A operação ADD só funciona se o utilizador for o Owner do workspace <ws>");
            System.out.println("[CLIENT] UP <ws> <file1> ... <filen> # Adicionar ficheiros ao workspace.");
            System.out.println("[CLIENT] DW <ws> <file1> ... <filen> # Download de ficheiros do workspace para a maquina local.");
            System.out.println("[CLIENT] RM <ws> <file1> ... <filen> # Apagar ficheiros do workspace.");
            System.out.println("[CLIENT] LW # Lista os workspaces associados ao utilizador.");
            System.out.println("[CLIENT] LS <ws> # Lista os ficheiros dentro de um workspace.");

            System.out.print("> ");
            String input = scanner.nextLine();
            String[] commands = input.split("\n");

            for (String command : commands) {
                String[] commandParts = command.split(" ");
                if (!checkCommandParts(commandParts)){
                    System.out.println("[CLIENT] Comando possui caracteres nao alfanumericos: " + command);
                    System.out.println("[CLIENT] Comando nao executado: " + command);
                    continue;
                }
                String commandAction = commandParts[0].toUpperCase();

                switch (commandAction) {
                    case "CREATE":
                        if (commandParts.length == 2) {
                            networkManager.createWorkspace(commandParts[1]);
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: CREATE");
                        }
                        break;
                    case "ADD":
                        if (commandParts.length == 3) {
                            // add user to workspace
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: ADD");
                        }
                        break;
                    case "UP":
                        if (commandParts.length >= 3) {
                            // upload files to workspace
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: UP");
                        }
                        break;
                    case "DW":
                        if (commandParts.length >= 3) {
                            // download files from workspace
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: DW");
                        }
                        break;
                    case "RM":
                        if (commandParts.length >= 3) {
                            // remove files from workspace
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: RM");
                        }
                        break;
                    case "LW":
                        if (commandParts.length == 1) {
                            // list workspaces
                        } else {
                            System.err.println("[CLIENT] Uso incorreto do comando: LW");
                        }
                        break;
                    case "LS":
                        if (commandParts.length == 2) {
                            // list files in workspace
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
     * Check if all command parts are alphanumeric
     *
     * @param commandParts the input to be checked
     * @return true if the input is alphanumeric, false otherwise
     */
    private boolean checkCommandParts(String[] commandParts){
        for (String commandPart : commandParts) {
            if (!isAlfaNumeric(commandPart)) {
                return false;
            }
        }
        return true;
    }
}
