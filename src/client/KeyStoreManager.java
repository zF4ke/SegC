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



/*
 * Nota:
 * Comando usado para gerar par de chaves, onde a chave publica vai para um certeficado, 
 * indo isto tudo para dentro de uma keytool, esta que é gerada diretamente se ainda não
 * existir o ficheiro
 * 
 * all the passwords are 123456 
 * 
 * Cliente:
 * Nome da keyStore: myKeys
 * Alias: keyRSA
 * 
 * keytool-genkeypair-alias keyRSA -keyalg RSA -keysize 2048 -storetype JCEKS keystore myKeys
 * 
 * Servidor:
 * Nome da keyStore: serverKeys
 * Alias: keyRSA
 * 
 * keytool -genkeypair -alias keyRSA -keyalg RSA -keysize 2048 -storetype JCEKS -keystore serverKeys
 * 
 * Export Certificate from serverKeys
 * 
 * keytool -exportcert -alias keyRSA -storetype JCEKS -keystore serverKeys -file keyRSA.cer
 * 
 * 
 * Cliente:
 * 
 * Now we need to create a TrustStore in the client with the certificate that can be found 
 * in the server with the following command:
 * 
 *  keytool -importcert -alias myserver -file ../../server/chaves/keyRSA.cer -keystore trustStore -storetype JCEKS
 *   
 *  Now i will create a certificate for the public key from the client 
 * 
 *  keytool -exportcert -alias keyRSA -storetype JCEKS -keystore serverKeys -file keyRSA.cer
 * 
 */

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
            e.printStackTrace();
        } catch (CertificateException e) {
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
