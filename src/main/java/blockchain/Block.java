package blockchain;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Block implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int index;
    private final long timestamp;
    private final String previousHash;
    private final List<Transaction> transactions;
    private String merkleRoot;
    private String hash;
    private int nonce;

    public Block(int index, String previousHash) {
        this.index = index;
        this.timestamp = Instant.now().getEpochSecond();
        this.previousHash = previousHash;
        this.transactions = new ArrayList<>();
        this.nonce = 0;
        this.merkleRoot = calculateMerkleRoot();
        this.hash = calculateHash();
    }

    public String calculateHash() {
        try {
            String dataToHash = index + timestamp + previousHash + merkleRoot + nonce;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculating block hash", e);
        }
    }

    public void mineBlock(int difficulty) {
        String target = new String(new char[difficulty]).replace('\0', '0');
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
        }
        System.out.println("Block mined: " + hash);
    }

    public boolean addTransaction(Transaction transaction) {
        if (transaction == null) return false;
        
        transactions.add(transaction);
        merkleRoot = calculateMerkleRoot();
        hash = calculateHash();
        return true;
    }

    private String calculateMerkleRoot() {
        if (transactions.isEmpty()) {
            return "0";
        }
        
        List<String> treeLayer = transactions.stream()
                .map(Transaction::getTransactionId)
                .toList();
        
        while (treeLayer.size() > 1) {
            List<String> newLayer = new ArrayList<>();
            for (int i = 0; i < treeLayer.size() - 1; i += 2) {
                newLayer.add(hashPair(treeLayer.get(i), treeLayer.get(i + 1)));
            }
            if (treeLayer.size() % 2 == 1) {
                newLayer.add(hashPair(treeLayer.get(treeLayer.size() - 1), treeLayer.get(treeLayer.size() - 1)));
            }
            treeLayer = newLayer;
        }
        
        return treeLayer.get(0);
    }

    private String hashPair(String hash1, String hash2) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = hash1 + hash2;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculating hash pair", e);
        }
    }

    public boolean validateBlock() {
        return hash.equals(calculateHash());
    }

    // Getters
    public int getIndex() {
        return index;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getHash() {
        return hash;
    }

    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }
}
