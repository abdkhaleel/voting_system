package voting;

import blockchain.*;
import user.User;
import user.CryptoUtils;
import exception.VotingException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class VotingSystem {
    
    private final Blockchain blockchain;
    private final ElectionManager electionManager;
    private final AuditLog auditLog;
    
    public VotingSystem() {
        this.blockchain = new Blockchain(4); // Difficulty level 4
        this.electionManager = new ElectionManager();
        this.auditLog = new AuditLog();
        
        // Load existing elections
        electionManager.loadElections();
    }
    
    public Election createElection(String title, String description, LocalDateTime startDate, LocalDateTime endDate) {
        Election election = electionManager.createElection(title, description, startDate, endDate);
        auditLog.logEvent("Election created: " + election.getId());
        return election;
    }
    
    public void addCandidateToElection(String electionId, Candidate candidate) {
        Optional<Election> electionOpt = electionManager.getElection(electionId);
        
        if (electionOpt.isPresent()) {
            Election election = electionOpt.get();
            election.addCandidate(candidate);
            electionManager.updateElection(election);
            auditLog.logEvent("Candidate added to election: " + candidate.getId());
        } else {
            throw new VotingException("Election not found: " + electionId);
        }
    }
    
    public void addEligibleVoter(String electionId, String voterId) {
        Optional<Election> electionOpt = electionManager.getElection(electionId);
        
        if (electionOpt.isPresent()) {
            Election election = electionOpt.get();
            election.addEligibleVoter(voterId);
            electionManager.updateElection(election);
            auditLog.logEvent("Voter added to eligible list: " + voterId);
        } else {
            throw new VotingException("Election not found: " + electionId);
        }
    }
    
    public void castVote(String electionId, String candidateId, User voter) {
        Optional<Election> electionOpt = electionManager.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            throw new VotingException("Election not found: " + electionId);
        }
        
        Election election = electionOpt.get();
        
        // Check if election is active
        if (!election.isActive()) {
            throw new VotingException("Election is not active");
        }
        
        // Check if voter is eligible
        if (!election.isVoterEligible(voter.getId())) {
            throw new VotingException("Voter is not eligible for this election");
        }
        
        // Check if voter has already voted
        if (election.hasVoterVoted(voter.getId())) {
            throw new VotingException("Voter has already cast a vote in this election");
        }
        
        // Check if candidate exists
        boolean candidateExists = election.getCandidates().stream()
                .anyMatch(c -> c.getId().equals(candidateId));
        
        if (!candidateExists) {
            throw new VotingException("Candidate not found: " + candidateId);
        }
        
        // Create vote transaction
        VoteTransaction voteTransaction = new VoteTransaction(voter.getId(), electionId, candidateId);
        
        // Sign the transaction with voter's private key
        byte[] signature = CryptoUtils.sign(voteTransaction.getData(), voter.getPrivateKey());
        voteTransaction.signTransaction(signature);
        
        // Add transaction to blockchain
        blockchain.addTransaction(voteTransaction);
        
        // Mine pending transactions if there are enough
        if (blockchain.getPendingTransactions().size() >= 5) {
            blockchain.minePendingTransactions();
        }
        
        // Mark voter as having voted
        election.markVoterAsVoted(voter.getId());
        electionManager.updateElection(election);
        
        // Log the vote
        auditLog.logEvent("Vote cast by voter: " + voter.getId() + " in election: " + electionId);
    }
    
    public ElectionResults getElectionResults(String electionId) {
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
        
        auditLog.logEvent("Election results generated for: " + electionId);
        return results;
    }
    
    public void activateElection(String electionId) {
        Optional<Election> electionOpt = electionManager.getElection(electionId);
        
        if (electionOpt.isPresent()) {
            Election election = electionOpt.get();
            election.activate();
            electionManager.updateElection(election);
            auditLog.logEvent("Election activated: " + electionId);
        } else {
            throw new VotingException("Election not found: " + electionId);
        }
    }
    
    public void deactivateElection(String electionId) {
        Optional<Election> electionOpt = electionManager.getElection(electionId);
        
        if (electionOpt.isPresent()) {
            Election election = electionOpt.get();
            election.deactivate();
            electionManager.updateElection(election);
            auditLog.logEvent("Election deactivated: " + electionId);
        } else {
            throw new VotingException("Election not found: " + electionId);
        }
    }
    
    public void saveBlockchain(String filePath) {
        BlockchainPersistence.saveBlockchain(blockchain, filePath);
        auditLog.logEvent("Blockchain saved to: " + filePath);
    }
    
    public void loadBlockchain(String filePath) {
        Blockchain loadedBlockchain = BlockchainPersistence.loadBlockchain(filePath);
        // You might want to implement a proper way to replace the current blockchain
        // This is simplified
        auditLog.logEvent("Blockchain loaded from: " + filePath);
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
}
