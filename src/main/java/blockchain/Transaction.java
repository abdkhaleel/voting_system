package blockchain;

import java.io.Serializable;
import java.security.PublicKey;

public interface Transaction extends Serializable {
    String getTransactionId();
    byte[] getSignature();
    void signTransaction(byte[] signature);
    boolean verifySignature(PublicKey publicKey);
    String getData();
}
