package client.Tests;

import java.security.Key;
import java.security.KeyStore;
import java.security.PublicKey;

import client.KeyStoreManager;
import java.security.cert.Certificate;
import java.util.Base64;

import javax.crypto.Cipher;


public class TestKeys {
    public static void main(String[] args) {
        try {
            KeyStoreManager ksm = new KeyStoreManager();
            KeyStore kStore = ksm.createKeyStore("123456");

            Key privateKey = ksm.getPrivateKey();
            PublicKey publicKey = ksm.getPublicKey();
            Certificate cert = ksm.getCertificate();

            System.out.println("Private Key: " + privateKey);
            System.out.println("Public Key: " + publicKey);
            System.out.println("Certificate: " + cert);

            // Let's encrypt something
            String originalMessage = "Hello secure world!";
            String encrypted = encryptWithPublicKey(originalMessage, publicKey);
            String decrypted = decryptWithPrivateKey(encrypted, privateKey);

            System.out.println("Original Message: " + originalMessage);
            System.out.println("Encrypted (Base64): " + encrypted);
            System.out.println("Decrypted Message: " + decrypted);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     public static String encryptWithPublicKey(String message, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(message.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decryptWithPrivateKey(String encryptedBase64, Key privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
        return new String(decryptedBytes, "UTF-8");
    }

    private void testKeys(){
        try {
            KeyStoreManager ksm = new KeyStoreManager();
            KeyStore kStore = ksm.createKeyStore("123456");

            Key privateKey = ksm.getPrivateKey();
            PublicKey publicKey = ksm.getPublicKey();
            Certificate cert = ksm.getCertificate();

            System.out.println("Private Key: " + privateKey);
            System.out.println("Public Key: " + publicKey);
            System.out.println("Certificate: " + cert);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
