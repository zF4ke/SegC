package server;

import server.models.User;

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
     * Gets a user from the file.
     *
     * @param userId the user ID
     * @return the user, or null if the user does not exist
     */
    public User getUser(String userId) {
        try (Scanner scanner = new Scanner(new File(usersFilePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(":");

                if (parts.length != 2) {
                    System.err.println("[USER STORAGE] Formato inválido: " + line);
                    continue;
                }

                String username = parts[0];
                String password = parts[1];

                if (username.equals(userId)) {
                    return new User(userId, password);
                }
            }
        } catch (IOException e) {
            System.err.println("[USER STORAGE] Erro ao carregar usuários: " + e.getMessage());
        }

        return null;
    }

    /**
     * Add a user to the file.
     *
     * @param user the user
     * @param password the password
     * @return true if the user was added, false otherwise
     */
    public boolean addUser(String user, String password) {
        if (this.getUser(user) != null) {
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
