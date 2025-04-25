package blockchain;

import exception.VotingException;
import user.DatabaseService;
import voting.VoteRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BlockchainPersistence {
    private final DatabaseService dbService;
    private VoteRepository voteRepository;

    public BlockchainPersistence(DatabaseService dbService) {
        this.dbService = dbService;
        initializeTables();
    }
    
    public void setVoteRepository(VoteRepository voteRepository) {
        this.voteRepository = voteRepository;
    }

    private void initializeTables() {
        // Create blocks table
        String blocksSql = "CREATE TABLE IF NOT EXISTS blockchain_blocks (" +
                "hash VARCHAR(64) PRIMARY KEY, " +
                "previous_hash VARCHAR(64), " +
                "timestamp BIGINT NOT NULL, " +
                "nonce INT NOT NULL, " +
                "index_num INT NOT NULL, " +
                "merkle_root VARCHAR(64), " +
                "difficulty INT NOT NULL" +
                ")";

        // Create transactions table
        String transactionsSql = "CREATE TABLE IF NOT EXISTS blockchain_transactions (" +
                "hash VARCHAR(64) PRIMARY KEY, " +
                "block_hash VARCHAR(64), " +
                "type VARCHAR(50) NOT NULL, " +
                "data TEXT NOT NULL, " +
                "timestamp BIGINT NOT NULL, " +
                "signature BLOB, " +
                "FOREIGN KEY (block_hash) REFERENCES blockchain_blocks(hash) ON DELETE CASCADE" +
                ")";

        // Create vote transactions table for specific vote data
        String voteTransactionsSql = "CREATE TABLE IF NOT EXISTS blockchain_vote_transactions (" +
                "transaction_hash VARCHAR(64) PRIMARY KEY, " +
                "voter_id VARCHAR(36) NOT NULL, " +
                "election_id VARCHAR(36) NOT NULL, " +
                "candidate_id VARCHAR(36) NOT NULL, " +
                "FOREIGN KEY (transaction_hash) REFERENCES blockchain_transactions(hash) ON DELETE CASCADE" +
                ")";

        try (Connection conn = dbService.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(blocksSql)) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(transactionsSql)) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(voteTransactionsSql)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new VotingException("Failed to initialize blockchain tables: " + e.getMessage());
        }
    }

    public void saveBlockchain(Blockchain blockchain) {
        try (Connection conn = dbService.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Clear existing data
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM blockchain_blocks")) {
                    stmt.executeUpdate();
                }
                
                // Save each block
                for (Block block : blockchain.getChain()) {
                    saveBlock(conn, block, blockchain.getDifficulty());
                }
                
                // Save pending transactions
                for (Transaction tx : blockchain.getPendingTransactions()) {
                    saveTransaction(conn, tx, null);
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new VotingException("Failed to save blockchain: " + e.getMessage());
        }
    }

    private void saveBlock(Connection conn, Block block, int difficulty) throws SQLException {
        String blockSql = "INSERT INTO blockchain_blocks (hash, previous_hash, timestamp, nonce, index_num, merkle_root, difficulty) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(blockSql)) {
            stmt.setString(1, block.getHash());
            stmt.setString(2, block.getPreviousHash());
            stmt.setLong(3, block.getTimestamp());
            stmt.setInt(4, block.getNonce());
            stmt.setInt(5, block.getIndex());
            stmt.setString(6, block.getMerkleRoot());
            stmt.setInt(7, difficulty);
            stmt.executeUpdate();
        }
        
        // Save transactions in this block
        for (Transaction tx : block.getTransactions()) {
            saveTransaction(conn, tx, block.getHash());
            
            // Update vote records with block hash if VoteRepository is set
            if (voteRepository != null && tx instanceof VoteTransaction) {
                voteRepository.saveVote((VoteTransaction) tx, block.getHash());
            }
        }
    }

    private void saveTransaction(Connection conn, Transaction tx, String blockHash) throws SQLException {
        String txSql = "INSERT INTO blockchain_transactions (hash, block_hash, type, data, timestamp, signature) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        
        String txHash;
        if (tx instanceof VoteTransaction) {
            txHash = ((VoteTransaction) tx).calculateHash();
        } else {
            // For other transaction types, generate a hash from the data
            txHash = generateHashFromData(tx.getData());
        }
        
        try (PreparedStatement stmt = conn.prepareStatement(txSql)) {
            stmt.setString(1, txHash);
            stmt.setString(2, blockHash);
            stmt.setString(3, tx.getClass().getSimpleName());
            stmt.setString(4, tx.getData());
            
            // Get timestamp from transaction
            long timestamp = 0;
            if (tx instanceof VoteTransaction) {
                timestamp = ((VoteTransaction) tx).getTimestamp();
            }
            stmt.setLong(5, timestamp);
            
            stmt.setBytes(6, tx.getSignature());
            stmt.executeUpdate();
        }
        
        // Save specific transaction data based on type
        if (tx instanceof VoteTransaction) {
            saveVoteTransaction(conn, (VoteTransaction) tx);
        }
    }

    private String generateHashFromData(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating hash", e);
        }
    }

    private void saveVoteTransaction(Connection conn, VoteTransaction voteTx) throws SQLException {
        String voteTxSql = "INSERT INTO blockchain_vote_transactions (transaction_hash, voter_id, election_id, candidate_id) " +
                "VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(voteTxSql)) {
            stmt.setString(1, voteTx.calculateHash());
            stmt.setString(2, voteTx.getVoterId());
            stmt.setString(3, voteTx.getElectionId());
            stmt.setString(4, voteTx.getCandidateId());
            stmt.executeUpdate();
        }
    }

    public Blockchain loadBlockchain() {
        // First, get the difficulty from the first block
        int difficulty = 4; // Default difficulty
        
        try (Connection conn = dbService.getConnection()) {
            String difficultySql = "SELECT difficulty FROM blockchain_blocks LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(difficultySql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    difficulty = rs.getInt("difficulty");
                }
            }
        } catch (SQLException e) {
            System.out.println("Could not retrieve difficulty, using default: " + difficulty);
        }
        
        Blockchain blockchain = new Blockchain(difficulty);
        
        try (Connection conn = dbService.getConnection()) {
            // Load blocks
            String blocksSql = "SELECT * FROM blockchain_blocks ORDER BY index_num ASC";
            
            try (PreparedStatement stmt = conn.prepareStatement(blocksSql);
                 ResultSet rs = stmt.executeQuery()) {
                
                // Clear existing chain
                blockchain.getChain().clear();
                
                while (rs.next()) {
                    String hash = rs.getString("hash");
                    String previousHash = rs.getString("previous_hash");
                    long timestamp = rs.getLong("timestamp");
                    int nonce = rs.getInt("nonce");
                    int index = rs.getInt("index_num");
                    String merkleRoot = rs.getString("merkle_root");
                    
                    // Create block using the constructor that takes index and previousHash
                    Block block = new Block(index, previousHash);
                    
                    // Set the other properties manually
                    java.lang.reflect.Field timestampField = Block.class.getDeclaredField("timestamp");
                    timestampField.setAccessible(true);
                    timestampField.set(block, timestamp);
                    
                    block.setMerkleRoot(merkleRoot);
                    block.setHash(hash);
                    block.setNonce(nonce);
                    
                    // Load transactions for this block
                    loadTransactionsForBlock(conn, block);
                    
                    // Add block to chain
                    blockchain.getChain().add(block);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new SQLException("Failed to set block fields: " + e.getMessage());
            }
            
            // Load pending transactions
            loadPendingTransactions(conn, blockchain);
            
            return blockchain;
        } catch (SQLException e) {
            throw new VotingException("Failed to load blockchain: " + e.getMessage());
        }
    }

    private void loadTransactionsForBlock(Connection conn, Block block) throws SQLException {
        String txSql = "SELECT * FROM blockchain_transactions WHERE block_hash = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(txSql)) {
            stmt.setString(1, block.getHash());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transaction tx = loadTransaction(conn, rs);
                    if (tx != null) {
                        block.addTransaction(tx);
                    }
                }
            }
        }
    }

    private void loadPendingTransactions(Connection conn, Blockchain blockchain) throws SQLException {
        String txSql = "SELECT * FROM blockchain_transactions WHERE block_hash IS NULL";
        
        try (PreparedStatement stmt = conn.prepareStatement(txSql);
             ResultSet rs = stmt.executeQuery()) {
            
            blockchain.setPendingTransactions(new ArrayList<>());
            
            while (rs.next()) {
                Transaction tx = loadTransaction(conn, rs);
                if (tx != null) {
                    blockchain.getPendingTransactions().add(tx);
                }
            }
        }
    }

    private Transaction loadTransaction(Connection conn, ResultSet rs) throws SQLException {
        String hash = rs.getString("hash");
        String type = rs.getString("type");
        String data = rs.getString("data");
        long timestamp = rs.getLong("timestamp");
        byte[] signature = rs.getBytes("signature");
        
        Transaction tx = null;
        
        if ("VoteTransaction".equals(type)) {
            tx = loadVoteTransaction(conn, hash);
            
            // If we have a VoteTransaction, we need to set the signature
            if (tx != null && signature != null) {
                tx.signTransaction(signature);
            }
        } else {
            // We don't support other transaction types in this implementation
            System.out.println("Unsupported transaction type: " + type);
        }
        
        return tx;
    }

    private VoteTransaction loadVoteTransaction(Connection conn, String txHash) throws SQLException {
        String voteTxSql = "SELECT * FROM blockchain_vote_transactions WHERE transaction_hash = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(voteTxSql)) {
            stmt.setString(1, txHash);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String voterId = rs.getString("voter_id");
                    String electionId = rs.getString("election_id");
                    String candidateId = rs.getString("candidate_id");
                    
                    return new VoteTransaction(voterId, electionId, candidateId);
                }
            }
        }
        
        return null;
    }
}
