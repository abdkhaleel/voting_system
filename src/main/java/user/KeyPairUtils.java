package user;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyPairUtils {
    
    public static KeyPair fromEncodedKeys(byte[] publicKeyBytes, byte[] privateKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Error reconstructing key pair", e);
        }
    }
}
