package client.Tests;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class TrustStoreManager {
    
    private KeyStore trustStore;
    private TrustManagerFactory tmf;
    private String password; 

    public TrustStoreManager() {
        createTrustStore("123456");
    }

    private void createTrustStore(String password) {
        if (password == null){
            return;
        }
        try(FileInputStream kfile = new FileInputStream("src/client/chaves/myKeys")){
            trustStore = KeyStore.getInstance("JCEKS");
            this.password = password;
            char[] passwordChar = password.toCharArray();
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
 }
