package client;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

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

    public static PublicKey getUserPublicKeyFromKeyStore(String alias) {
        try {
            String keyStorePath = KEYSTORE_PATH + alias + ".keystore";

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
            String keyStorePath = KEYSTORE_PATH + alias + ".keystore";

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(keyStorePath), DEFAULT_PASSWORD.toCharArray());
            return (PrivateKey) keyStore.getKey(alias, DEFAULT_PASSWORD.toCharArray());
        } catch (Exception e) {
            System.err.println("[CLIENT SECURITY UTILS] Erro ao obter a chave privada do keystore: " + e.getMessage());
            return null;
        }
    }
}
