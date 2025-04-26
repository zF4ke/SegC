package client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

//imports for X509 certificate
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;



public class KeyStoreManager {

    public KeyStore kStore;
    private String password;
    
    public KeyStore createKeyStore(String password) throws KeyStoreException {
        generateKeyStore(password);


        return null;
    }

    private void generateKeyStore(String password) throws KeyStoreException {
        if (password == null){
            return;
        }
        this.password = password; 
 
        try(FileInputStream kfile = new FileInputStream("src/client/chaves/myKeys")){
            kStore = KeyStore.getInstance("JCEKS");
            char[] passwordChar = password.toCharArray();
            kStore.load(kfile, passwordChar);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    public Key getPrivateKey() {
        try {
            return kStore.getKey("keyRSA", password.toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            e.printStackTrace();
            return null; 
        }
    }

    public PublicKey getPublicKey() {
        return getCertificate().getPublicKey();
    }

    public Certificate getCertificate() {
        try {
            return kStore.getCertificate("keyRSA");
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return null;
        }
    }

    
}
