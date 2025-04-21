package voting;

import blockchain.*;
import user.User;
import user.CryptoUtils;
import user.DatabaseService;
import exception.VotingException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class VotingSystem {
    
    private final Blockchain blockchain;
    private final ElectionManager electionManager;
    private final AuditLog auditLog;
    private final CandidateService candidateService;
    
    public VotingSystem(DatabaseService dbService) {
        this.blockchain = new Blockchain(4); // Difficulty level 4
        this.auditLog = new AuditLog();
        this.electionManager = new ElectionManager(dbService, auditLog);
        this.candidateService = new CandidateService(dbService);
        
        // Load blockchain from storage if available
        try {
            Blockchain loadedChain = BlockchainPersistence.loadBlockchain("blockchain.dat");
            if (loadedChain != null) {
                // Copy the loaded chain's properties to our blockchain instance
                java.lang.reflect.Field chainField = Blockchain.class.getDeclaredField("chain");
                chainField.setAccessible(true);
                chainField.set(blockchain, loadedChain.getChain());
                
                java.lang.reflect.Field pendingTransactionsField = Blockchain.class.getDeclaredField("pendingTransactions");
                pendingTransactionsField.setAccessible(true);
                pendingTransactionsField.set(blockchain, loadedChain.getPendingTransactions());
                
                java.lang.reflect.Field difficultyField = Blockchain.class.getDeclaredField("difficulty");
                difficultyField.setAccessible(true);
                difficultyField.set(blockchain, loadedChain.getDifficulty());
                
                auditLog.logEvent("Blockchain loaded from storage");
            }
        } catch (Exception e) {
            auditLog.logEvent("Failed to load blockchain: " + e.getMessage());
            // Continue with a new blockchain
        }
    }
    
    public Election createElection(String title, String description, LocalDateTime startDate, LocalDateTime endDate) {
        Election election = electionManager.createElection(title, description, startDate, endDate);
        auditLog.logEvent("Election created: " + election.getId());
        return election;
    }
    
    public void addCandidateToElection(String electionId, String name, String party, String position) {
        // First check if the election exists
        Optional<Election> electionOpt = electionManager.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            throw new VotingException("Election not found: " + electionId);
        }
        
        // Create the candidate
        Candidate candidate = candidateService.createCandidate(electionId, name, party, position);
        auditLog.logEvent("Candidate added to election: " + candidate.getId());
    }
    
    public void updateCandidate(Candidate candidate, String electionId) {
        candidateService.updateCandidate(candidate, electionId);
        auditLog.logEvent("Candidate updated: " + candidate.getId());
    }
    
    public void removeCandidate(String candidateId, String electionId) {
        candidateService.deleteCandidate(candidateId, electionId);
        auditLog.logEvent("Candidate removed: " + candidateId);
    }
    
    public void addEligibleVoter(String electionId, String voterId) {
        electionManager.addEligibleVoter(electionId, voterId);
    }
    
    public void removeEligibleVoter(String electionId, String voterId) {
        electionManager.removeEligibleVoter(electionId, voterId);
    }
    
    public void castVote(String electionId, String candidateId, User voter) {
        // Check if election exists
        Optional<Election> electionOpt = electionManager.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            throw new VotingException("Election not found: " + electionId);
        }
        
        Election election = electionOpt.get();
        System.out.println("Caste vote method check " + election.isActive());
        // Check if election is active
        if (!election.isActive()) {
            throw new VotingException("Election is not active");
        }
        
        // Check if voter is eligible
        if (!electionManager.isVoterEligible(electionId, voter.getId())) {
            throw new VotingException("Voter is not eligible for this election");
        }
        
        // Check if voter has already voted
        if (electionManager.hasVoterVoted(electionId, voter.getId())) {
            throw new VotingException("Voter has already cast a vote in this election");
        }
        
        // Check if candidate exists
        Optional<Candidate> candidateOpt = candidateService.getCandidate(candidateId);
        if (candidateOpt.isEmpty()) {
            throw new VotingException("Candidate not found: " + candidateId);
        }
        
        // Create vote transaction
        VoteTransaction voteTransaction = new VoteTransaction(voter.getId(), electionId, candidateId);
        
        // Sign the transaction with voter's private key
        byte[] signature = CryptoUtils.sign(voteTransaction.getData(), voter.getPrivateKey());
        voteTransaction.signTransaction(signature);
        
        // Add transaction to blockchain
        blockchain.addTransaction(voteTransaction);
        
        // Mark voter as having voted
        electionManager.markVoterAsVoted(electionId, voter.getId());
        
        // Mine pending transactions if there are enough
        if (blockchain.getPendingTransactions().size() >= 5) {
            blockchain.minePendingTransactions();
            // Save blockchain after mining
            saveBlockchain();
        }
        
        // Log the vote
        auditLog.logEvent("Vote cast by voter: " + voter.getId() + " in election: " + electionId);
    }
    
    public ElectionResults getElectionResults(String electionId) {
        // Check if election exists
        Optional<Election> electionOpt = electionManager.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            throw new VotingException("Election not found: " + electionId);
        }
        
        Election election = electionOpt.get();
        
        // Create election results
        ElectionResults results = new ElectionResults(election);
        
        // Count votes from blockchain
        List<Block> blocks = blockchain.getChain();
        
        for (Block block : blocks) {
            for (Transaction transaction : block.getTransactions()) {
                if (transaction instanceof VoteTransaction) {
                    VoteTransaction voteTransaction = (VoteTransaction) transaction;
                    
                    if (voteTransaction.getElectionId().equals(electionId)) {
                        results.countVote(voteTransaction.getCandidateId());
                    }
                }
            }
        }
        
        // Also count votes from pending transactions
        for (Transaction transaction : blockchain.getPendingTransactions()) {
            if (transaction instanceof VoteTransaction) {
                VoteTransaction voteTransaction = (VoteTransaction) transaction;
                
                if (voteTransaction.getElectionId().equals(electionId)) {
                    results.countVote(voteTransaction.getCandidateId());
                }
            }
        }
        
        auditLog.logEvent("Election results generated for: " + electionId);
        return results;
    }
    
    public void activateElection(String electionId) {
        electionManager.activateElection(electionId);
    }
    
    public void deactivateElection(String electionId) {
        electionManager.deactivateElection(electionId);
    }
    
    public void saveBlockchain() {
        try {
            BlockchainPersistence.saveBlockchain(blockchain, "blockchain.dat");
            auditLog.logEvent("Blockchain saved to storage");
        } catch (Exception e) {
            auditLog.logEvent("Failed to save blockchain: " + e.getMessage());
            throw new VotingException("Failed to save blockchain: " + e.getMessage());
        }
    }
    
    public void loadBlockchain() {
        try {
            Blockchain loadedChain = BlockchainPersistence.loadBlockchain("blockchain.dat");
            if (loadedChain != null) {
                // Copy the loaded chain's properties to our blockchain instance
                java.lang.reflect.Field chainField = Blockchain.class.getDeclaredField("chain");
                chainField.setAccessible(true);
                chainField.set(blockchain, loadedChain.getChain());
                
                java.lang.reflect.Field pendingTransactionsField = Blockchain.class.getDeclaredField("pendingTransactions");
                pendingTransactionsField.setAccessible(true);
                pendingTransactionsField.set(blockchain, loadedChain.getPendingTransactions());
                
                java.lang.reflect.Field difficultyField = Blockchain.class.getDeclaredField("difficulty");
                difficultyField.setAccessible(true);
                difficultyField.set(blockchain, loadedChain.getDifficulty());
                
                auditLog.logEvent("Blockchain loaded from storage");
            }
        } catch (Exception e) {
            auditLog.logEvent("Failed to load blockchain: " + e.getMessage());
            throw new VotingException("Failed to load blockchain: " + e.getMessage());
        }
    }
    
    public List<Election> getAllElections() {
        return electionManager.getAllElections();
    }
    
    public List<Election> getActiveElections() {
        return electionManager.getActiveElections();
    }
    
    public Optional<Election> getElection(String electionId) {
        return electionManager.getElection(electionId);
    }
    
    public List<Candidate> getCandidatesForElection(String electionId) {
        return candidateService.getCandidatesForElection(electionId);
    }
    
    public Optional<Candidate> getCandidate(String candidateId) {
        return candidateService.getCandidate(candidateId);
    }
    
    public boolean isVoterEligible(String electionId, String voterId) {
        return electionManager.isVoterEligible(electionId, voterId);
    }
    
    public boolean hasVoterVoted(String electionId, String voterId) {
        return electionManager.hasVoterVoted(electionId, voterId);
    }
    
    // Getters
    public Blockchain getBlockchain() {
        return blockchain;
    }
    
    public ElectionManager getElectionManager() {
        return electionManager;
    }
    
    public AuditLog getAuditLog() {
        return auditLog;
    }
    
    public boolean validateBlockchain() {
        return blockchain.isChainValid();
    }
    
    public void mineBlockchain() {
        blockchain.minePendingTransactions();
        saveBlockchain();
        auditLog.logEvent("Blockchain mined and saved");
    }
}

