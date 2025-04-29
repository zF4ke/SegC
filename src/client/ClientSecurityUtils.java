package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

public class ClientSecurityUtils {
    
    /*
     * This are the current supported signatures in java 
     * SHA1withDSA
     * SHA1withRSA
     * SHA256withRSA
     */

    private static final String ALGORITHM = "SHA256withRSA";


    /**
     * Creates a signed file using the given private key.
     *
     * @param filePath   the path to the file to be signed
     * @param privateKey the private key to be used for signing
     * @return the path to the signed file
     */
    //TODO check if the file is created in the same directory as the original file
    public static File createSignedFile(String filePath, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);

            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            signature.update(fileBytes);

            byte[] signedBytes = signature.sign();

            System.out.println("Signature length: " + signedBytes.length);


            // String base64EncodedBytes = Base64.getEncoder().encodeToString(signedBytes);
            // System.out.println("Base64 encoded signature length: " + base64EncodedBytes.length());

            Path signaturePath = Paths.get(filePath + ".signedFile");

            //TODO check where the file is created
            Files.createFile(signaturePath);
            Files.write(signaturePath, signedBytes);
            return signaturePath.toFile();

        
        } catch (Exception e) {
            System.err.println("[CLIENT] Error while signing the file: " + e.getMessage());
            System.err.println("[CLIENT] System compromised! Shutting down...");
            System.exit(1);
        }
        return null;
    }


    /**
     * Verifies the signature of a file using the given public key.
     *
     * @param filePath          the path to the file to be verified
     * @param signatureFilePath the path to the signature file
     * @param publicKey         the public key to be used for verification
     * @return true if the signature is valid, false otherwise
     */
    //TODO check if the file is created in the same directory as the original file
    public static boolean verifySignedFile(String filePath, String signatureFilePath, PublicKey publicKey) {
        try {
            File signatureFile = new File(signatureFilePath);
            if (!signatureFile.exists()) {
                System.err.println("[SERVER] Signature file does not exist: " + signatureFilePath);
                return false;
            }
    
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
    
            // Read the data file
            byte[] dataBytes = Files.readAllBytes(Paths.get(filePath));
            System.out.println("File size: " + dataBytes.length);
    
            // // Read the Base64 encoded signature file
            // String base64EncodedSignature = new String(Files.readAllBytes(Paths.get(signatureFilePath)));
            // System.out.println("Base64 Encoded Signature Length: " + base64EncodedSignature.length());
    
            // // Decode the Base64 encoded signature
            // byte[] decodedSignature = Base64.getDecoder().decode(base64EncodedSignature);
            // System.out.println("Decoded signature length: " + decodedSignature.length);
    
            // // Extract only the first 256 bytes (the expected size for SHA256 with RSA signature)
            // byte[] signatureBytes = Arrays.copyOfRange(decodedSignature, 0, 256);
            // System.out.println("Extracted signature length: " + signatureBytes.length);
            
            // Read the signature file
            byte[] signatureBytes = Files.readAllBytes(Paths.get(signatureFilePath));

            // Verify the signature
            signature.update(dataBytes);
            return signature.verify(signatureBytes);
    
        } catch (Exception e) {
            System.err.println("[CLIENT] Error while verifying the signed file: " + e.getMessage());
            e.printStackTrace();
            System.err.println("[CLIENT] System compromised! Shutting down...");
            System.exit(1);
        }
        return false;
    }
    

    //genrate java doc explain what the function does
    /**
     * Encrypts a file using the given key.
     *
     * @param filePath the path to the file to be encrypted
     * @param key      the key to be used for encryption
     * @return the path to the encrypted file
     */
    //TODO check if the file is created in the same directory as the original file
    public static String encriptFile(String filePath, Key key) {
        try {
            //Bocado de codigo que vai buscar a chave á keystore
            /* 
            KeyStoreManager ksm = new KeyStoreManager();
            KeyStore kStore = ksm.createKeyStore("123456");
            PrivateKey privateKey = ksm.getPrivateKey();
            PublicKey publicKey = ksm.getPublicKey();
            */
    
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);
    
            FileInputStream fis;
            FileOutputStream fos;
            CipherOutputStream cos;

            String encryptedPath = filePath + ".cif";
    
            fis = new FileInputStream(filePath);
            fos = new FileOutputStream(encryptedPath);

            cos = new CipherOutputStream(fos, cipher);
            byte[] b = new byte[16];
            int i = fis.read(b);
            while (i != -1) {
                cos.write(b, 0, i);
                i = fis.read(b);
            }

            cos.close();
            fis.close();
            fos.close();


            return encryptedPath;

        } catch (Exception e) {
            System.err.println("[CLIENT] Error while encrypting the file: " + e.getMessage());
            System.err.println("[CLIENT] System compromised! Shutting down...");
            System.exit(1);
        }
        return null;
    }

    /**
     * Decrypts a file using the given key.
     *
     * @param filePath the path to the file to be decrypted
     * @param key      the key to be used for decryption
     * @return the path to the decrypted file
     */
    //TODO check if the file is created in the same directory as the original file
    public static String decriptFile(String filePath, Key key) {

		try {
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, key);

            FileInputStream fis;
            FileOutputStream fos;
            CipherInputStream cis;

            String decryptedPath;

            // This will remove the ".cif" extension from the file name added in the encryptFile method
            if (filePath.endsWith(".cif")) {
                decryptedPath = filePath.substring(0, filePath.length() - 4); 
            } else {
                throw new IllegalArgumentException("File is not encrypted (.cif extension missing)");
            }
            fis = new FileInputStream(filePath);
            fos = new FileOutputStream(decryptedPath);
            cis = new CipherInputStream(fis, c);

            byte[] b = new byte[16];
            int i = cis.read(b);
            while (i != -1) {
                fos.write(b, 0, i);
                i = cis.read(b);
            }
            
            fos.close();
		    cis.close();

        } catch (Exception e) {
            System.err.println("[CLIENT] Error while decrypting the file: " + e.getMessage());
            System.err.println("[CLIENT] System compromised! Shutting down...");
            System.exit(1);
        }
        return null;
    }

}
