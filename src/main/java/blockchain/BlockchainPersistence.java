package blockchain;

import java.io.*;

public class BlockchainPersistence {
    
    public static void saveBlockchain(Blockchain blockchain, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(blockchain);
            System.out.println("Blockchain saved to: " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving blockchain: " + e.getMessage());
            throw new RuntimeException("Failed to save blockchain", e);
        }
    }
    
    public static Blockchain loadBlockchain(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("No existing blockchain found. Creating new blockchain.");
            return new Blockchain(4); // Default difficulty
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Blockchain blockchain = (Blockchain) ois.readObject();
            System.out.println("Blockchain loaded from: " + filePath);
            return blockchain;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading blockchain: " + e.getMessage());
            System.out.println("Creating new blockchain instead.");
            return new Blockchain(4); // Default difficulty
        }
    }
}
