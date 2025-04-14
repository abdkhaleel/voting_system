package user;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class CryptoUtils {
    
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
            keyGen.initialize(ecSpec, random);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Error generating key pair", e);
        }
    }
    
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    public static String hashPassword(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    public static byte[] sign(String data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException("Error signing data", e);
        }
    }
    
    public static boolean verify(String data, byte[] signature, PublicKey publicKey) {
        try {
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(data.getBytes(StandardCharsets.UTF_8));
            return ecdsaVerify.verify(signature);
        } catch (Exception e) {
            throw new RuntimeException("Error verifying signature", e);
        }
    }
}
