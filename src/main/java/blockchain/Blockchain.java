package blockchain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Blockchain implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final List<Block> chain;
    private final int difficulty;
    private List<Transaction> pendingTransactions;

    public Blockchain(int difficulty) {
        this.chain = new ArrayList<>();
        this.difficulty = difficulty;
        this.pendingTransactions = new ArrayList<>();
        
        // Create the genesis block
        createGenesisBlock();
    }

    private void createGenesisBlock() {
        Block genesisBlock = new Block(0, "0");
        genesisBlock.mineBlock(difficulty);
        chain.add(genesisBlock);
        System.out.println("Genesis block created: " + genesisBlock.getHash());
    }

    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public void addTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        pendingTransactions.add(transaction);
        System.out.println("Transaction added to pending: " + transaction.getTransactionId());
    }

    public void minePendingTransactions() {
        if (pendingTransactions.isEmpty()) {
            System.out.println("No pending transactions to mine");
            return;
        }
        
        Block newBlock = new Block(chain.size(), getLatestBlock().getHash());
        
        // Add pending transactions to the block
        for (Transaction transaction : pendingTransactions) {
            newBlock.addTransaction(transaction);
        }
        
        // Mine the block
        System.out.println("Mining block...");
        newBlock.mineBlock(difficulty);
        
        // Add the block to the chain
        chain.add(newBlock);
        System.out.println("Block successfully mined and added to the chain");
        
        // Clear pending transactions
        pendingTransactions = new ArrayList<>();
    }
    

    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);
            
            // Validate block hash
            if (!currentBlock.validateBlock()) {
                System.out.println("Block #" + i + " has invalid hash");
                return false;
            }
            
            // Validate chain integrity
            if (!currentBlock.getPreviousHash().equals(previousBlock.getHash())) {
                System.out.println("Block #" + i + " has invalid previous hash reference");
                return false;
            }
        }
        
        return true;
    }

	public List<Transaction> getPendingTransactions() {
		return pendingTransactions;
	}

	public void setPendingTransactions(List<Transaction> pendingTransactions) {
		this.pendingTransactions = pendingTransactions;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public List<Block> getChain() {
		return chain;
	}

	public int getDifficulty() {
		return difficulty;
	}
    
    
}
