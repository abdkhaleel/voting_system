package blockchain;

import java.util.ArrayList;
import java.util.List;

public class ConsensusManager {
    private final List<Blockchain> networkNodes;
    
    public ConsensusManager() {
        this.networkNodes = new ArrayList<>();
    }
    
    public void addNode(Blockchain node) {
        networkNodes.add(node);
    }
    
    public Blockchain resolveConflicts() {
        Blockchain longestChain = null;
        int maxLength = 0;
        
        // Find the longest valid chain
        for (Blockchain node : networkNodes) {
            if (node.isChainValid() && node.getChain().size() > maxLength) {
                maxLength = node.getChain().size();
                longestChain = node;
            }
        }
        
        // If we found a longer valid chain, replace our chain
        if (longestChain != null) {
            System.out.println("Consensus reached. Chain replaced.");
            return longestChain;
        }
        
        System.out.println("Current chain is valid and up-to-date.");
        return networkNodes.get(0); // Return the first node if no longer chain found
    }
}
