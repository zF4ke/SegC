package server.utils;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class SecurityUtils {
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 256;
    public static final int DEFAULT_ITERATION_COUNT = 10000;
    public static final String MAC_ALGORITHM = "HmacSHA256";

    public static byte[] genSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return salt;
    }

    public static SecretKey genSecretKey(String password, byte[] salt, int iterationCount)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, HASH_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec);
    }

    public static String genSecurePassword(String userId, String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = genSalt();
        SecretKey secretKey = genSecretKey(password, salt, DEFAULT_ITERATION_COUNT);
        String hashString = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        String saltString = Base64.getEncoder().encodeToString(salt);
        return userId + ":" + hashString + ":" + saltString;
    }

    public static boolean verifyPassword(String inputPassword, String storedUser)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        String[] parts = storedUser.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Password tem que ter formato: userId:hash:salt");
        }

        String storedHashB64 = parts[1];
        String saltB64 = parts[2];

        byte[] storedHash = Base64.getDecoder().decode(storedHashB64);
        byte[] salt = Base64.getDecoder().decode(saltB64);

        SecretKey secretKey = genSecretKey(inputPassword, salt, DEFAULT_ITERATION_COUNT);
        byte[] inputHash = secretKey.getEncoded();
        return MessageDigest.isEqual(inputHash, storedHash);
    }

    public static byte[] genMac(byte[] data, SecretKey key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(MAC_ALGORITHM);
        mac.init(key);
        return mac.doFinal(data);
    }

    public static boolean verifyMac(byte[] expectedMac, byte[] actualMac)
            throws Exception {
        return MessageDigest.isEqual(expectedMac, actualMac);
    }

    public static void writeMacOnMacFile(Path filePath, byte[] mac) throws Exception {
        Files.write(filePath, Base64.getEncoder().encode(mac));
    }

    public static byte[] readMacFromMacFile(Path filePath) throws Exception {
        if (Files.exists(filePath)) {
            return Base64.getDecoder().decode(Files.readAllBytes(filePath));
        }
        return null;
    }

    public static byte[] genFileMac(Path filePath, SecretKey key) throws Exception {
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("Ficheiro nao encontrado: " + filePath);
        }
        byte[] fileBytes = Files.readAllBytes(filePath);
        return genMac(fileBytes, key);
    }

    public static boolean verifyFileMac(Path filePath, Path macFilePath, SecretKey key) 
        throws Exception {
        if (!Files.exists(filePath) || !Files.exists(macFilePath)) {
            return false;
        }
        byte[] expectedMac = readMacFromMacFile(macFilePath);
        if (expectedMac == null) {
            throw new IllegalArgumentException("MAC nao encontrado no ficheiro: " + macFilePath);
        }
        byte[] actualMac = genFileMac(filePath, key);
        return verifyMac(expectedMac, actualMac);
    }


    //TODO this is from the SecutityUtils class found in the client, but it was neede in the server
    public static final String ALGORITHM = "SHA256withRSA";

        /**
     * Verifies the signature of a file using the given public key.
     *
     * @param filePath          the path to the file to be verified
     * @param signatureFilePath the path to the signature file
     * @param publicKey         the public key to be used for verification
     * @return true if the signature is valid, false otherwise
     */
    //TODO check if the file is created in the same directory as the original file
    public static boolean verifySignedFile(String filePath, String signatureFilePath  ,PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);

            byte[] dataBytes = Files.readAllBytes(Paths.get(filePath));
            byte[] signedBytes = Files.readAllBytes(Paths.get(signatureFilePath));
            signature.update(dataBytes);

            return signature.verify(signedBytes);

        } catch (Exception e) {
            System.err.println("[CLIENT] Error while verifying the signed file: " + e.getMessage());
            System.err.println("[CLIENT] System compromised! Shutting down...");
            System.exit(1);
        }
        return false;
    }

}
