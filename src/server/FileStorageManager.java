package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import server.models.Workspace;

/*
* workspaces.txt example:
* workspace001:owner_username:user1,user2,user3,user4
*
*
*/

public class FileStorageManager {
    private static FileStorageManager INSTANCE;
    private final String DATA_DIR_PATH = "data/";
    private final String WORKSPACES_FILE_PATH = "data/workspaces.txt";
    private final String WORKSPACES_DIR_PATH = "data/workspaces/";

    /**
     * Create a new file storage manager.
     */
    private FileStorageManager() {
        try {
            // Create directory paths if they don't exist
            Path dataDir = Paths.get(DATA_DIR_PATH);
            Path workspacesDir = Paths.get(WORKSPACES_DIR_PATH);
            Path workspacesFile = Paths.get(WORKSPACES_FILE_PATH);

            Files.createDirectories(dataDir);
            Files.createDirectories(workspacesDir);

            // Create workspaces file only if it doesn't exist
            if (!Files.exists(workspacesFile)) {
                Files.createFile(workspacesFile);
            }
        } catch (IOException e){
            System.out.println("[FILE STORAGE] Erro ao criar diretórios e arquivos: " + e.getMessage());
        }

    }

    /**
     * Get the instance of the file storage manager.
     *
     * @return the instance
     */
    public synchronized static FileStorageManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FileStorageManager();
        }
        
        return INSTANCE;
    }

    ////////////////////////////////////////////////////////////
    
    public Boolean createWorkspace(String username, String workspace) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new java.io.FileWriter(WORKSPACES_FILE_PATH, true))) { 
            //adding the workspace to the workspace.txt file
            String newLine = workspace + ":" + username + ":" + username;
            bufferedWriter.write(newLine);
            bufferedWriter.newLine();

            //creating the dir for the workspace
            new File(WORKSPACES_DIR_PATH + workspace).mkdirs();

        } catch (IOException e) {
            System.out.println("Error creating BufferedWriter to write a new workspace in workspaces.txt: " + e.getMessage());
            return false;
        }

        return true;
    }

    public boolean addUserToWorkspace(String username, String Workspace)  {
        //TODO
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(WORKSPACES_FILE_PATH));
             BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(WORKSPACES_FILE_PATH));)

        {
            
        } catch (Exception e) {
        }
        
        return false;
    }

    public boolean[] uploadFiles(String workspace, String[] filePaths) { 
        boolean[] uploaded = new boolean[filePaths.length];
        try {
            Path workSpacePath = Paths.get(WORKSPACES_DIR_PATH + workspace);
            for (int i = 0; i < filePaths.length; i++) {
                Files.move(Paths.get(filePaths[i]), workSpacePath, StandardCopyOption.REPLACE_EXISTING);
                uploaded[i] = true;
            }
        
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uploaded;
    }

    public String[] downloadFiles(String workspace, String[] fileNames) {
        String[] paths = new String[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            Path path = Paths.get(WORKSPACES_DIR_PATH + workspace + "/" + fileNames[i]);
            if (Files.exists(path)) {
                paths[i] = path.toString();
            }
        }
        return paths;
    }

    public boolean [] removeFiles(String workspace, String[] fileNames) {
        boolean[] filesRemoved = new boolean[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            File file = new File(workspace + fileNames[i]);
            filesRemoved[i] = file.delete();
        }
        
        return filesRemoved;
    }

    public String[] listWorkspaces(String username){
        try(Scanner scanner = new Scanner(new File(WORKSPACES_FILE_PATH))) {
            List<String> list = new ArrayList<>();

            while(scanner.hasNextLine()) {
                Workspace w = createWorkspace(scanner.nextLine());
                if (w.isMember(username)){
                    list.add(w.getName());
                }
            }
            return list.toArray(String[]:: new);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Invalid format off workspace");
            e.printStackTrace();
        }

        return new String[0];
    }

    public String[] listFiles(String workspace) {
        try (Scanner scanner = new Scanner(new File(WORKSPACES_FILE_PATH))){
            while (scanner.hasNextLine()) { 
                Workspace w = createWorkspace(scanner.nextLine());
                if ( w.getName().equals(workspace) ){
                    File dir = new File(WORKSPACES_DIR_PATH + workspace);
                    return dir.list();
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new String[0];
    }


    ///////////////////////////////////////////////////////////
    
    public Boolean workspaceExists(String workspace) {
       return Files.exists(Paths.get(WORKSPACES_DIR_PATH + workspace));
    }
    
    public boolean isUserInWorkspace(String username, String workspace) {
        
        try (Scanner scanner = new Scanner(new File(WORKSPACES_FILE_PATH))){
            while (scanner.hasNextLine()) { 
                Workspace w = createWorkspace(scanner.nextLine());
                if (w.getName().equals(workspace) && w.isMember(username)){
                    return true;
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } 
        
        return false;
    }

    public boolean isUserOwner(String ownerUsername, String workspace) {
        try (Scanner scanner = new Scanner(new File(WORKSPACES_FILE_PATH))){
            while (scanner.hasNextLine()) { 
                Workspace w = createWorkspace(scanner.nextLine());
                if (w == null) {
                    return false;
                } 
                if (w.getName().equals(workspace) && w.getOwnerUsername().equals(ownerUsername)){
                    return true;
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } 
        return false;
    }


    private Workspace createWorkspace(String line) {
        try {
            String[] lineSplit = line.split(":");
            if (lineSplit.length != 3) {
                throw new IOException(); 
            }
            else {
            return new Workspace(lineSplit[0], lineSplit[1], java.util.Arrays.asList(lineSplit[2].split(",")));
            }
        } catch (IOException e) {
            System.out.println("[WORKSPACE INVALID FORMAT] Erro ao ler worksapces " + e.getMessage());
            return null;
        }

    }

}