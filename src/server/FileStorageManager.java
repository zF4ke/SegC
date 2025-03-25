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

/*
* workspaces.txt example:
* workspace001:owner_username:user1,user2,user3,user4
*
*
*/

public class FileStorageManager {

    private static FileStorageManager Instance;
    private final String WORKSPACES_FILE_PATH = "data/workspaces.txt";
    private final String DATA_DIR_PATH = "src/server/data/";
    private final String WORKSPACES_DIR_PATH = "src/server/data/workspaces/";


    private FileStorageManager() {
        try {
            //create a dir for the data if it doesnt exist already
            if (!Files.exists(Paths.get(DATA_DIR_PATH))) {
                new File(DATA_DIR_PATH).mkdirs();
            }
            //create a dir for workspaces inside of data inside of server: src/server/data/workspaces
            if (!Files.exists(Paths.get(WORKSPACES_DIR_PATH))) {
                new File(WORKSPACES_DIR_PATH).mkdirs();
            }

            File workspaces = new File(WORKSPACES_FILE_PATH);
            workspaces.createNewFile(); //if it already exists doenst create a thing

        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public synchronized static FileStorageManager getInstance() {
        if (Instance == null) {
            Instance = new FileStorageManager();
        }
        
        return Instance;
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
            if (Files.move(Paths.get(filePaths[i]), workSpacePath,  StandardCopyOption.REPLACE_EXISTING) != null) {
                uploaded[i] = true;
            } 
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
            File file = new File(fileNames[i]);
            filesRemoved[i] = file.delete();
        }
        
        return filesRemoved;
    }

    public String[] listWorkspaces(String username){
        try(Scanner scanner = new Scanner(new File(WORKSPACES_FILE_PATH))) {
            List<String> list = new ArrayList<>();

            while(scanner.hasNextLine()) {
                String[] line = scanner.nextLine().split(":");
                if (line.length != 3) {
                    throw new IOException();
                }
                if (isUserInLine(line[3], username)){
                    list.add(line[0]);
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
                String[] line = scanner.nextLine().split(":");
                if (line.length < 2 && line[0].equals(workspace) ){
                    return line[3].split(",");
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
        //TODO apagar
        //Nota: inicialmente tinha feito com line.contains(), era mais simples mas vulneravel a ataques, username podia ser parte de outro
        // etc
        try (Scanner scanner = new Scanner(new File(WORKSPACES_FILE_PATH))){
            while (scanner.hasNextLine()) { 
                String[] line = scanner.nextLine().split(":");
                if (line.length != 3) {
                    throw new IOException(); 
                }
                if (line[0].equals(workspace) && isUserInLine(line[3], username)){
                    return true;
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Invalid format off workspace");
            e.printStackTrace();
        }
        
        return false;
    }

    public boolean isUserOwner(String username, String workspace) {
        try (Scanner scanner = new Scanner(new File(WORKSPACES_FILE_PATH))){
            while (scanner.hasNextLine()) { 
                String[] line = scanner.nextLine().split(":");
                if (line.length < 2 && line[0].equals(workspace) && line[1].equals(username)){
                    return true;
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }
    

    private boolean isUserInLine(String line, String username) {
        for (String user : line.split(",")) {
            if (user.equals(username)) {
                return true;
            }
        }

        return false;
    }

}