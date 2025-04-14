package blockchain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;

public class VoteTransaction implements Transaction {
    private static final long serialVersionUID = 1L;
    
    private final String transactionId;
    private final String voterId;
    private final String electionId;
    private final String candidateId;
    private final long timestamp;
    private byte[] signature;

    public VoteTransaction(String voterId, String electionId, String candidateId) {
        this.voterId = voterId;
        this.electionId = electionId;
        this.candidateId = candidateId;
        this.timestamp = Instant.now().getEpochSecond();
        this.transactionId = calculateHash();
    }

    private String calculateHash() {
        try {
            String data = voterId + electionId + candidateId + timestamp;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculating transaction hash", e);
        }
    }

    @Override
    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public byte[] getSignature() {
        return signature;
    }

    @Override
    public void signTransaction(byte[] signature) {
        this.signature = signature;
    }

    @Override
    public boolean verifySignature(PublicKey publicKey) {
        try {
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(getData().getBytes(StandardCharsets.UTF_8));
            return ecdsaVerify.verify(signature);
        } catch (Exception e) {
            throw new RuntimeException("Error verifying transaction signature", e);
        }
    }

    @Override
    public String getData() {
        return voterId + electionId + candidateId + timestamp;
    }

    public String getVoterId() {
        return voterId;
    }

    public String getElectionId() {
        return electionId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "VoteTransaction{" +
                "transactionId='" + transactionId + '\'' +
                ", voterId='" + voterId + '\'' +
                ", electionId='" + electionId + '\'' +
                ", candidateId='" + candidateId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
    
}
