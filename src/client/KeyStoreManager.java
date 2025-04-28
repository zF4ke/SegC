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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;



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

public class KeyStoreManager {

    public KeyStore kStore;
    private final String PASSWORD = "123456";
    
    public  KeyStoreManager(String keypath) {
        generateKeyStore(keypath);
    }

    private void generateKeyStore(String keypath)  {
       
 
        try(FileInputStream kfile = new FileInputStream(keypath)){
            kStore = KeyStore.getInstance("PKCS12");
            char[] passwordChar = PASSWORD.toCharArray();
            kStore.load(kfile, passwordChar);
            if (kStore != null) {
                System.out.println("Keystore contains the alias: " + kStore.containsAlias("keyrsa"));
            }
            


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
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        
    }

    public PrivateKey getPrivateKey() {
        try {
            return (PrivateKey) kStore.getKey("keyrsa", PASSWORD.toCharArray());
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
            return kStore.getCertificate("keyrsa");
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return null;
        }
    }

}
