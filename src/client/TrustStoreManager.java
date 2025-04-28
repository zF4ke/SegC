package client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class TrustStoreManager {
    
    private KeyStore trustStore;
    private TrustManagerFactory tmf;
    private final String PASSWORD = "123456"; 
    private final String ALIAS_USER_CERTIFICATE = "myClient";
    private final String ALIAS_SERVER_CERTIFICATE = "myServer";

    public TrustStoreManager(String keypath) {
        createTrustStore(keypath);
    }

    private void createTrustStore(String keypath) {
        try(FileInputStream kfile = new FileInputStream(keypath)) {
            trustStore = KeyStore.getInstance("PKCS12");
           
            char[] passwordChar = PASSWORD.toCharArray();
            trustStore.load(kfile, passwordChar);
            
            //this is then used by the TLS to make the secure conection i believe
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }

    }

    public TrustManager[] getTrustManagers() {
        return tmf.getTrustManagers();
    }

    public Certificate getCertificate(String alias) {
        try {
            return trustStore.getCertificate(alias);
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return null;
        }
    }

    public PublicKey getClientPublicKey() {
        return getCertificate(ALIAS_USER_CERTIFICATE).getPublicKey();
    }
 }
