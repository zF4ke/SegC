package client.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public class SecurityUtils {
    
    /*
     * This are the current supported signatures in java 
     * SHA1withDSA
     * SHA1withRSA
     * SHA256withRSA
     */

    private static final String ALGORITHM = "SHA256withRSA";


    public File createSignedFile(String filePath, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);

            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            signature.update(fileBytes);

            byte[] signedBytes = signature.sign();
            Path signaturePath = Paths.get(filePath + ".signedFile");

            //TODO check where the file is created
            Files.createFile(signaturePath);
            Files.write(Paths.get(filePath), signedBytes);
            return signaturePath.toFile();

        } catch (NoSuchAlgorithmException e) {
            // The algorithm used in the signature does not exist
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // The private key used is invalid for some reason
            e.printStackTrace();
        } catch (IOException e) {
            // For the file that has been read
            e.printStackTrace();
        } catch (SignatureException e) {
            // Error caught while updating the signature with the fileBytes
            e.printStackTrace();
        }

        return null;
    }


    public boolean verifySignedFile(String filePath, String signatureFilePath  ,PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);

            byte[] dataBytes = Files.readAllBytes(Paths.get(filePath));
            byte[] signedBytes = Files.readAllBytes(Paths.get(signatureFilePath));
            signature.update(dataBytes);

            return signature.verify(signedBytes);

        } catch (NoSuchAlgorithmException e) {
            // The algorithm used in the signature does not exist
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // Error while initializing the key 
            e.printStackTrace();
        } catch (IOException e) {
            // Error while reading the data from the key 
            e.printStackTrace();
        } catch (SignatureException e) {
            // Error while "updating" loading the file data
            e.printStackTrace();
        }

        return false;
    } 
}
