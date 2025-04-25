package server.utils;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class SecurityUtils {
    public static final int DEFAULT_ITERATION_COUNT = 100000;

    public static byte[] genSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[100];
        sr.nextBytes(salt);
        return salt;
    }

    public static SecretKey genSecretKey(String password, byte[] salt, int iterationCount)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_256");
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
            throw new IllegalArgumentException("Stored password must have format: userId:hash:salt");
        }

        String storedHashB64 = parts[1];
        String saltB64 = parts[2];

        byte[] storedHash = Base64.getDecoder().decode(storedHashB64);
        byte[] salt = Base64.getDecoder().decode(saltB64);

        SecretKey secretKey = genSecretKey(inputPassword, salt, DEFAULT_ITERATION_COUNT);
        byte[] inputHash = secretKey.getEncoded();
        return MessageDigest.isEqual(inputHash, storedHash);
    }
}
