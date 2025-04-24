package blockchain;

import user.DatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BlockchainRepository {
    private final DatabaseService databaseService;

    public BlockchainRepository(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public void saveBlockchain(Blockchain blockchain) {
        try {
            // First, ensure we have a blockchain record
            ensureBlockchainExists(blockchain.getDifficulty());
            
            // Save all blocks in the chain
            for (Block block : blockchain.getChain()) {
                saveBlock(block);
            }
            
            // Save pending transactions
            savePendingTransactions(blockchain.getPendingTransactions());
            
            System.out.println("Blockchain saved to database successfully");
        } catch (SQLException e) {
            System.err.println("Error saving blockchain to database: " + e.getMessage());
            throw new RuntimeException("Failed to save blockchain to database", e);
        }
    }

    private void ensureBlockchainExists(int difficulty) throws SQLException {
        String checkSql = "SELECT id FROM blockchain LIMIT 1";
        List<Integer> ids = DatabaseService.executeQuery(checkSql, rs -> rs.getInt("id"));
        
        if (ids.isEmpty()) {
            String insertSql = "INSERT INTO blockchain (difficulty) VALUES (?)";
            DatabaseService.executeUpdate(insertSql, difficulty);
        }
    }

    private void saveBlock(Block block) throws SQLException {
        String sql = "INSERT INTO blocks (hash, index_num, previous_hash, timestamp, merkle_root, nonce) " +
                     "VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "previous_hash = VALUES(previous_hash), " +
                     "timestamp = VALUES(timestamp), " +
                     "merkle_root = VALUES(merkle_root), " +
                     "nonce = VALUES(nonce)";
        
        DatabaseService.executeUpdate(
            sql, 
            block.getHash(),
            block.getIndex(),
            block.getPreviousHash(),
            block.getTimestamp(),
            block.getMerkleRoot(),
            block.getHash().hashCode() // Using hash code as nonce for simplicity
        );
        
        // Save all transactions in this block
        for (Transaction transaction : block.getTransactions()) {
            saveTransaction(transaction, block.getHash(), false);
        }
    }

    private void savePendingTransactions(List<Transaction> pendingTransactions) throws SQLException {
        // First, mark all pending transactions as non-pending
        String updateSql = "UPDATE transactions SET is_pending = FALSE WHERE is_pending = TRUE";
        DatabaseService.executeUpdate(updateSql);
        
        // Then save the current pending transactions
        for (Transaction transaction : pendingTransactions) {
            saveTransaction(transaction, null, true);
        }
    }

    private void saveTransaction(Transaction transaction, String blockHash, boolean isPending) throws SQLException {
        if (!(transaction instanceof VoteTransaction)) {
            throw new IllegalArgumentException("Only VoteTransaction is supported");
        }
        
        VoteTransaction voteTransaction = (VoteTransaction) transaction;
        
        String sql = "INSERT INTO transactions (transaction_id, block_hash, voter_id, election_id, candidate_id, timestamp, signature, is_pending) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "block_hash = VALUES(block_hash), " +
                     "is_pending = VALUES(is_pending)";
        
        DatabaseService.executeUpdate(
            sql,
            voteTransaction.getTransactionId(),
            blockHash,
            voteTransaction.getVoterId(),
            voteTransaction.getElectionId(),
            voteTransaction.getCandidateId(),
            voteTransaction.getTimestamp(),
            voteTransaction.getSignature(),
            isPending
        );
    }

    public Blockchain loadBlockchain() {
        try {
            // Get blockchain metadata
            String blockchainSql = "SELECT difficulty FROM blockchain LIMIT 1";
            List<Integer> difficulties = DatabaseService.executeQuery(blockchainSql, rs -> rs.getInt("difficulty"));
            
            int difficulty = difficulties.isEmpty() ? 4 : difficulties.get(0);
            
            // Create a new blockchain with the stored difficulty
            Blockchain blockchain = new Blockchain(difficulty);
            
            // Clear the genesis block that was automatically created
            blockchain.getChain().clear();
            
            // Load all blocks
            String blocksSql = "SELECT * FROM blocks ORDER BY index_num ASC";
            List<Block> blocks = DatabaseService.executeQuery(blocksSql, this::mapResultSetToBlock);
            
            // Add blocks to the chain
            for (Block block : blocks) {
                // Load transactions for this block
                loadTransactionsForBlock(block);
                blockchain.getChain().add(block);
            }
            
            // Load pending transactions
            List<Transaction> pendingTransactions = loadPendingTransactions();
            blockchain.setPendingTransactions(pendingTransactions);
            
            System.out.println("Blockchain loaded from database successfully");
            return blockchain;
        } catch (SQLException e) {
            System.err.println("Error loading blockchain from database: " + e.getMessage());
            System.out.println("Creating new blockchain instead.");
            return new Blockchain(4); // Default difficulty
        }
    }

 // Inside the mapResultSetToBlock method
    private Block mapResultSetToBlock(ResultSet rs) throws SQLException {
        return new Block(
            rs.getInt("index_num"),
            rs.getLong("timestamp"),
            rs.getString("previous_hash"),
            rs.getString("merkle_root"),
            rs.getString("hash"),
            rs.getInt("nonce")
        );
    }


    private void loadTransactionsForBlock(Block block) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE block_hash = ? AND is_pending = FALSE";
        List<Transaction> transactions = DatabaseService.executeQuery(
            sql,
            rs -> mapResultSetToTransaction(rs),
            block.getHash()
        );
        
        // Add transactions to the block
        for (Transaction transaction : transactions) {
            block.addTransaction(transaction);
        }
    }

    private List<Transaction> loadPendingTransactions() throws SQLException {
        String sql = "SELECT * FROM transactions WHERE is_pending = TRUE";
        return DatabaseService.executeQuery(sql, rs -> mapResultSetToTransaction(rs));
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        VoteTransaction transaction = new VoteTransaction(
            rs.getString("voter_id"),
            rs.getString("election_id"),
            rs.getString("candidate_id")
        );
        
        // Set the signature if it exists
        byte[] signature = rs.getBytes("signature");
        if (signature != null) {
            transaction.signTransaction(signature);
        }
        
        return transaction;
    }
}
