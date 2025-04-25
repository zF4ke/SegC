package server;

import server.models.User;
import server.utils.SecurityUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

                if (parts.length != 3) {
                    System.err.println("[USER STORAGE] Formato inválido: " + line);
                    continue;
                }

                String username = parts[0];
                String hash = parts[1];
                String salt = parts[2];

                if (username.equals(userId)) {
                    return new User(username, hash, salt);
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

        try {
            String securePassword = SecurityUtils.genSecurePassword(user, password);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE_PATH, true))) {
                writer.write(securePassword);
                writer.newLine();
            }
            return true;
        } catch (Exception e) {
            System.err.println("[USER STORAGE] Erro ao adicionar utilizador: " + e.getMessage());
            return false;
        }
    }

    /**
     * Remove a user from the file.
     *
     * @param user the user
     * @return true if the user was removed, false otherwise
     */
    public boolean removeUser(String user) {
        File inputFile = new File(USERS_FILE_PATH);
        File tempFile = new File(USERS_FILE_PATH + ".tmp");

        try (Scanner scanner = new Scanner(inputFile);
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            boolean userFound = false;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(":");

                // Skip the user we want to remove
                if (parts.length >= 1 && parts[0].equals(user)) {
                    userFound = true;
                    continue;
                }

                writer.write(line);
                writer.newLine();
            }

            if (!userFound) {
                tempFile.delete();
                return true;
            }

            // Replace original file with the new one
            if (!tempFile.renameTo(inputFile)) {
                Files.move(tempFile.toPath(), inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
        } catch (IOException e) {
            System.err.println("[USER STORAGE] Erro ao remover usuário: " + e.getMessage());
            return false;
        }
    }
}
