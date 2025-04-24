package blockchain;

import user.DatabaseService;

import java.io.*;

public class BlockchainPersistence {
    private final BlockchainRepository repository;
    
    public BlockchainPersistence(DatabaseService databaseService) {
        this.repository = new BlockchainRepository(databaseService);
    }
    
    public void saveBlockchain(Blockchain blockchain) {
        repository.saveBlockchain(blockchain);
    }
    
    public Blockchain loadBlockchain() {
        return repository.loadBlockchain();
    }
    
    // Keep the file-based methods for backup purposes
    public static void saveBlockchainToFile(Blockchain blockchain, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(blockchain);
            System.out.println("Blockchain saved to file: " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving blockchain to file: " + e.getMessage());
            throw new RuntimeException("Failed to save blockchain to file", e);
        }
    }
    
    public static Blockchain loadBlockchainFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("No existing blockchain file found. Creating new blockchain.");
            return new Blockchain(4); // Default difficulty
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Blockchain blockchain = (Blockchain) ois.readObject();
            System.out.println("Blockchain loaded from file: " + filePath);
            return blockchain;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading blockchain from file: " + e.getMessage());
            System.out.println("Creating new blockchain instead.");
            return new Blockchain(4); // Default difficulty
        }
    }
}
