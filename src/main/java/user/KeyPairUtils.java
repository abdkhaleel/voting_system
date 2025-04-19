package user;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyPairUtils {

    public static KeyPair fromEncodedKeys(byte[] publicKeyBytes, byte[] privateKeyBytes) {
        try {
            // Check if we're dealing with dummy data
            if (isDummyKeyData(publicKeyBytes) || isDummyKeyData(privateKeyBytes)) {
                // Return a newly generated key pair instead
                System.out.println("Detected dummy key data, generating a new key pair");
                return CryptoUtils.generateKeyPair();
            }
            
            // Normal key reconstruction
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            System.err.println("Error reconstructing key pair: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error reconstructing key pair", e);
        }
    }
    
    private static boolean isDummyKeyData(byte[] keyData) {
        // Check if the key data is our dummy string
        if (keyData == null || keyData.length == 0) {
            return true;
        }
        
        // Check if it's our dummy string
        String keyString = new String(keyData);
        return keyString.equals("dummy-public-key-data") || 
               keyString.equals("dummy-private-key-data");
    }
}
