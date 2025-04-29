package client;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;

/*
 * Nota:
 * Comandos usado para gerar par de chaves, certificados, keyStore e trustStore
 *
 * all the passwords are 123456
 *
 * Cliente:
 * Nome da keyStore: myKeys
 * Alias: keyRSA
 *
 * keytool -genkeypair -alias keyRSA -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.client
 *
 * Export Certificate from myKeys
 *
 * keytool -exportcert -alias keyRSA -storetype PKCS12 -keystore keystore.client -file keyRSA.client.cer
 *
 *
 * Servidor:
 * Nome da keyStore: serverKeys
 * Alias: keyRSA
 *
 * keytool -genkeypair -alias keyRSA -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.server
 *
 * Export Certificate from serverKeys
 *
 * keytool -exportcert -alias keyRSA -storetype PKCS12 -keystore keystore.server -file keyRSA.server.cer
 *
 *
 *
 *  TrustStore no cliente:
 *
 * Now we need to create a TrustStore in the client with the certificate that can be found
 * in the server with the following command:
 *
 *  keytool -importcert -alias myserver -file ../server_keys/keyRSA.server.cer -keystore truststore.client -storetype PKCS12
 *
 * adding the client key to the trustStore
 *
 *  keytool -importcert -alias myclient -file keyRSA.client.cer -keystore truststore.client -storetype PKCS12
 *
 *
 *
 * TrustStore no servidor:
 *
 * add the client key to the trustStore
 * keytool -importcert -alias myclient -file ../client_keys/keyRSA.client.cer -keystore truststore.server -storetype PKCS12
 *
 * adding the server key to the trustStore
 * keytool -importcert -alias myserver -file keyRSA.server.cer -keystore truststore.server -storetype PKCS12
 *
 *
 */

public class ClientSecurityUtils {
    private static final String KEYSTORE_PATH = "client_keys/";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String ALGORITHM = "SHA256withRSA";

    public static PublicKey getUserPublicKeyFromKeyStore(String alias) {
        try {
            String keyStorePath = KEYSTORE_PATH + alias + "/" + alias  + ".keystore";

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(keyStorePath), DEFAULT_PASSWORD.toCharArray());
            return keyStore.getCertificate(alias).getPublicKey();
        } catch (Exception e) {
            System.err.println("[CLIENT SECURITY UTILS] Erro ao obter a chave pública do keystore: " + e.getMessage());
            return null;
        }
    }

    public static PrivateKey getUserPrivateKeyFromKeyStore(String alias) {
        try {
            String keyStorePath = KEYSTORE_PATH + alias + "/" + alias  + ".keystore";

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(keyStorePath), DEFAULT_PASSWORD.toCharArray());
            return (PrivateKey) keyStore.getKey(alias, DEFAULT_PASSWORD.toCharArray());
        } catch (Exception e) {
            System.err.println("[CLIENT SECURITY UTILS] Erro ao obter a chave privada do keystore: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a signed file using the given private key.
     *
     * @param filePath   the path to the file to be signed
     * @param privateKey the private key to be used for signing
     * @return the path to the signed file
     */
    //TODO check if the file is created in the same directory as the original file
    public static File createSignedFile(String filePath, String userID,PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);

            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            signature.update(fileBytes);

            byte[] signedBytes = signature.sign();

            System.out.println("Signature length: " + signedBytes.length);


            // String base64EncodedBytes = Base64.getEncoder().encodeToString(signedBytes);
            // System.out.println("Base64 encoded signature length: " + base64EncodedBytes.length());

            Path signaturePath = Paths.get(filePath + ".signed." + userID);

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
