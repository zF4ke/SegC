package server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * A class to manage the users file.
 */
public class UserStorageManager {
    private final String usersFilePath;

    /**
     * Create a new user storage manager.
     *
     * @param usersFilePath the path to the users file
     */
    public UserStorageManager(String usersFilePath) {
        this.usersFilePath = usersFilePath;

        File file = new File(usersFilePath);
        if (!file.exists()) {
            try {
                boolean result = file.createNewFile();
                if (!result) {
                    throw new IOException();
                }
            } catch (IOException e) {
                System.err.println("[USER STORAGE] Erro ao criar arquivo de usuários: " + e.getMessage());
            }
        }
    }

    /**
     * Load the users from the file.
     *
     * @return the users
     */
    public Map<String, String> loadUsers() {
        Map<String, String> users = new HashMap<>();

        try (Scanner scanner = new Scanner(new File(usersFilePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(":");

                if (parts.length != 2) {
                    System.err.println("[USER STORAGE] Formato inválido: " + line);
                    continue;
                }

                users.put(parts[0].trim(), parts[1].trim());
            }
        } catch (IOException e) {
            System.err.println("[USER STORAGE] Erro ao carregar usuários: " + e.getMessage());
        }

        return users;
    }

    /**
     * Add a user to the file.
     *
     * @param user the user
     * @param password the password
     * @return true if the user was added, false otherwise
     */
    public boolean addUser(String user, String password) {
        Map<String, String> users = loadUsers();

        if (users.containsKey(user)) {
            System.err.println("[USER STORAGE] Usuário já existe: " + user);

            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(usersFilePath, true))) {
            writer.write(user + ":" + password);
            writer.newLine();

            return true;
        } catch (IOException e) {
            System.err.println("[USER STORAGE] Erro ao adicionar usuário: " + e.getMessage());

            return false;
        }
    }
}
