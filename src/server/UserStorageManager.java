package server;

import server.models.User;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * A class to manage the users file.
 */
public class UserStorageManager {
    private static UserStorageManager INSTANCE;
    private static final String USERS_FILE_PATH = "data/users.txt";

    /**
     * Create a new user storage manager.
     */
    private UserStorageManager() {
        try {
            Path file = Paths.get(USERS_FILE_PATH);
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException e) {
            System.err.println("[USER STORAGE] Erro ao inicializar ficheiro de usuários: " + e.getMessage());
        }
    }

    /**
     * Get the instance of the user storage manager.
     *
     * @return the instance
     */
    public synchronized static UserStorageManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UserStorageManager();
        }

        return INSTANCE;
    }

    /**
     * Gets a user from the file.
     *
     * @param userId the user ID
     * @return the user, or null if the user does not exist
     */
    public User getUser(String userId) {
        try (Scanner scanner = new Scanner(new File(USERS_FILE_PATH))) {
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

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE_PATH, true))) {
            writer.write(user + ":" + password);
            writer.newLine();

            return true;
        } catch (IOException e) {
            System.err.println("[USER STORAGE] Erro ao adicionar usuário: " + e.getMessage());

            return false;
        }
    }
}
