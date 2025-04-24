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
    private final BlockchainPersistence blockchainPersistence;
    
    public VotingSystem(DatabaseService dbService) {
        this.auditLog = new AuditLog();
        this.electionManager = new ElectionManager(dbService, auditLog);
        this.blockchainPersistence = new BlockchainPersistence(dbService);
        this.blockchain = blockchainPersistence.loadBlockchain();
        this.candidateService = new CandidateService(dbService);
        
        auditLog.logEvent("VotingSystem initialized with database-backed blockchain");
    }
    
    // Election Management
    public Election createElection(String title, String description, LocalDateTime startDate, LocalDateTime endDate) {
        Election election = electionManager.createElection(title, description, startDate, endDate);
        auditLog.logEvent("Election created: " + election.getId());
        return election;
    }
    
    public Election createElection(String title, String description, String startDateStr, String endDateStr) {
    	LocalDateTime startDate = LocalDateTime.parse(startDateStr);
        LocalDateTime endDate = LocalDateTime.parse(endDateStr);
    	return electionManager.createElection(title, description, startDate, endDate);
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
    
    public void activateElection(String electionId) {
        electionManager.activateElection(electionId);
        auditLog.logEvent("Election activated: " + electionId);
    }
    
    public void deactivateElection(String electionId) {
        electionManager.deactivateElection(electionId);
        auditLog.logEvent("Election deactivated: " + electionId);
    }
    
    // Candidate Management
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
    
    public List<Candidate> getCandidatesForElection(String electionId) {
        return candidateService.getCandidatesForElection(electionId);
    }
    
    public Optional<Candidate> getCandidate(String candidateId) {
        return candidateService.getCandidate(candidateId);
    }
    
    // Voter Management
    public void addEligibleVoter(String electionId, String voterId) {
        electionManager.addEligibleVoter(electionId, voterId);
        auditLog.logEvent("Voter added to election: " + voterId);
    }
    
    public void removeEligibleVoter(String electionId, String voterId) {
        electionManager.removeEligibleVoter(electionId, voterId);
        auditLog.logEvent("Voter removed from election: " + voterId);
    }
    
    public boolean isVoterEligible(String electionId, String voterId) {
        return electionManager.isVoterEligible(electionId, voterId);
    }
    
    public boolean hasVoterVoted(String electionId, String voterId) {
        return electionManager.hasVoterVoted(electionId, voterId);
    }
    
    // Voting Process
    public void castVote(String electionId, String candidateId, User voter) {
        // Check if election exists
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
            saveBlockchainState();
        }
        
        // Log the vote
        auditLog.logEvent("Vote cast by voter: " + voter.getId() + " in election: " + electionId);
        
        // Save the blockchain state after adding the transaction
        saveBlockchainState();
    }
    
    // Results and Blockchain Management
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
    
    public void mineBlockchain() {
        blockchain.minePendingTransactions();
        saveBlockchainState();
        auditLog.logEvent("Blockchain mined and saved");
    }
    
    public boolean validateBlockchain() {
        return blockchain.isChainValid();
    }
    
    // Blockchain Persistence
    public void saveBlockchainState() {
        blockchainPersistence.saveBlockchain(blockchain);
        auditLog.logEvent("Blockchain state saved to database");
    }
    
    public void shutdown() {
        saveBlockchainState();
        auditLog.logEvent("VotingSystem shutdown complete");
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
