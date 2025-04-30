package client;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Base64;

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
    private static final String KEYS_PATH = "client_keys/";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String ALGORITHM = "SHA256withRSA";

    public static PublicKey getUserPublicKeyFromKeyStore(String alias) {
        try {
            String keyStorePath = KEYS_PATH + alias + "/" + alias  + ".keystore";

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
            String keyStorePath = KEYS_PATH + alias + "/" + alias  + ".keystore";

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(keyStorePath), DEFAULT_PASSWORD.toCharArray());
            return (PrivateKey) keyStore.getKey(alias, DEFAULT_PASSWORD.toCharArray());
        } catch (Exception e) {
            System.err.println("[CLIENT SECURITY UTILS] Erro ao obter a chave privada do keystore: " + e.getMessage());
            return null;
        }
    }

    public static Certificate getUserCertificateFromTrustStore(String userId, String alias) {
        try {
            String trustStorePath = KEYS_PATH + userId + "/" + userId  + ".truststore";

            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(new FileInputStream(trustStorePath), DEFAULT_PASSWORD.toCharArray());
            return trustStore.getCertificate(alias);
        } catch (Exception e) {
            System.err.println("[CLIENT SECURITY UTILS] Erro ao obter o certificado do truststore: " + e.getMessage());
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
    public static File createSignedFile(String filePath, String userID, PrivateKey privateKey) {
        try {
            File dataFile = new File(filePath);
            if (!dataFile.exists()) {
                System.err.println("[CLIENT] Arquivo de dados não encontrado: " + filePath);
                return null;
            }

            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);

            // Read and hash the data file
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            signature.update(fileBytes);

            // Generate the signature
            byte[] signedBytes = signature.sign();

            // Create the signature file
            String nameWithoutEnc = filePath.substring(0, filePath.lastIndexOf("."));
            String signatureFileName = nameWithoutEnc + ".signed." + userID;
            Path signaturePath = Paths.get(signatureFileName);
            Files.write(signaturePath, signedBytes);

            return signaturePath.toFile();

        } catch (Exception e) {
            System.err.println("[CLIENT] Erro ao assinar o arquivo: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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
            //System.out.println("File size: " + dataBytes.length);

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


    /**
     * Encrypts a file using the given key.
     *
     * @param filePath the path to the file to be encrypted
     * @param keyFile the file containing the key
     * @param userId the user ID of the key
     * @return the path to the encrypted file
     */
    public static String encryptFile(String filePath, File keyFile, String userId) {
        try {
            // Step 1: Read and parse key file: expected format <salt>:<wrappedKey>
            // read and trim key file to remove any trailing whitespace or newline
            String keyData = new String(Files.readAllBytes(keyFile.toPath()), StandardCharsets.UTF_8).trim();
            String[] parts = keyData.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid key file format");
            }

            // decode wrapped AES key and salt: <wrappedKey>:<salt>
            byte[] wrappedAesKey = Base64.getDecoder().decode(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);

            // Step 2: Load private RSA key of the owner
            PrivateKey ownerPrivateKey = getUserPrivateKeyFromKeyStore(userId);

            // Step 3: Unwrap the AES key using RSA/OAEP
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, ownerPrivateKey);
            byte[] aesBytes = rsaCipher.doFinal(wrappedAesKey);
            SecretKey aesKey = new SecretKeySpec(aesBytes, "AES");

            // Step 4: Init AES cipher for encryption
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[16];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);

            // Step 5: Create output stream & write IV + salt first
            String encryptedPath = filePath + ".enc";
            try (FileOutputStream fos = new FileOutputStream(encryptedPath);
                 CipherOutputStream cos = new CipherOutputStream(fos, aesCipher);
                 FileInputStream fis = new FileInputStream(filePath)) {

                fos.write(iv);    // Write IV first
                fos.write(salt);  // Then write salt

                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, read);
                }
            }

            return encryptedPath;

        } catch (Exception e) {
            System.err.println("[CLIENT] Error while encrypting the file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypts a file previously encrypted with AES using the corresponding workspace key.
     * The file must begin with [16 bytes IV][SALT][AES-Ciphered content...].
     *
     * @param encryptedFilePath the path to the .enc file
     * @param keyFile           the workspace key file (e.g., ws001.key.jose)
     * @param userId            the ID of the user performing decryption
     * @return the path to the decrypted file, or null on failure
     */
    public static String decryptFile(String encryptedFilePath, File keyFile, String userId) {
        try {
            //System.out.println(keyFile);

            // Step 1: Read and parse key file (format: <wrappedKey>:<salt>)
            String keyData = new String(Files.readAllBytes(keyFile.toPath()), StandardCharsets.UTF_8).trim();
            String[] parts = keyData.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid key file format");
            }

            // decode wrapped AES key and salt: <wrappedKey>:<salt>
            byte[] wrappedAesKey = Base64.getDecoder().decode(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);

            // Step 2: Load user's private RSA key
            PrivateKey privateKey = getUserPrivateKeyFromKeyStore(userId);

            // Step 3: Unwrap AES key with RSA/OAEP
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(wrappedAesKey);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // Step 4: Read IV and salt from encrypted file
            FileInputStream fis = new FileInputStream(encryptedFilePath);
            byte[] iv = new byte[16];
            if (fis.read(iv) != 16) {
                fis.close();
                throw new IOException("Failed to read IV");
            }

            // Skip salt (we already got it from the key file, no need to re-parse it)
            fis.skip(salt.length);

            // Step 5: Init AES cipher
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);

            // Step 6: Stream decryption to output file
            // move original file to backup
            String outputPath = encryptedFilePath + ".dec";
            try (CipherInputStream cis = new CipherInputStream(fis, aesCipher);
                 FileOutputStream fos = new FileOutputStream(outputPath)) {

                byte[] buffer = new byte[4096];
                int read;
                while ((read = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }

            // delete the original encrypted file and replace it with decrypted content
            Files.delete(Paths.get(encryptedFilePath));
            Files.move(Paths.get(outputPath), Paths.get(encryptedFilePath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            // return the path to the decrypted file (now at the original encryptedFilePath)
            return encryptedFilePath;

        } catch (Exception e) {
            System.err.println("[CLIENT] Error while decrypting the file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
