package voting;

import blockchain.*;
import user.User;
import java.util.*;
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
    private final ElectionResultsRepository resultsRepository;
    private final VoteRepository voteRepository;
    
    public VotingSystem(DatabaseService dbService) {
        this.auditLog = new AuditLog();
        this.electionManager = new ElectionManager(dbService, auditLog);
        this.blockchainPersistence = new BlockchainPersistence(dbService);
        this.blockchain = blockchainPersistence.loadBlockchain();
        this.candidateService = new CandidateService(dbService);
        this.voteRepository = new VoteRepository(dbService, auditLog);
        this.resultsRepository = new ElectionResultsRepository(dbService, auditLog);
        
        // Set the vote repository in blockchain persistence
        this.blockchainPersistence.setVoteRepository(this.voteRepository);
        
        auditLog.logEvent("VotingSystem initialized with database-backed blockchain and vote storage");
    }
    
    /**
     * Gets detailed voting statistics for an election
     */
    public Map<String, Object> getElectionStatistics(String electionId) {
        Map<String, Object> statistics = new HashMap<>();
        
        // Get the election
        Optional<Election> electionOpt = electionManager.getElection(electionId);
        if (electionOpt.isEmpty()) {
            throw new VotingException("Election not found: " + electionId);
        }
        
        Election election = electionOpt.get();
        
        // Basic election info
        statistics.put("electionId", election.getId());
        statistics.put("title", election.getTitle());
        statistics.put("startDate", election.getStartDate());
        statistics.put("endDate", election.getEndDate());
        statistics.put("active", election.isActive());
        
        // Voter statistics
        statistics.put("eligibleVoterCount", election.getEligibleVoterIds().size());
        statistics.put("votedVoterCount", election.getVotedVoterIds().size());
        
        double turnoutPercentage = 0;
        if (election.getEligibleVoterIds().size() > 0) {
            turnoutPercentage = (election.getVotedVoterIds().size() * 100.0) / election.getEligibleVoterIds().size();
        }
        statistics.put("turnoutPercentage", turnoutPercentage);
        
        // Get results
        ElectionResults results = getElectionResults(electionId);
        
        // Candidate statistics
        List<Map<String, Object>> candidateStats = new ArrayList<>();
        for (Candidate candidate : election.getCandidates()) {
            Map<String, Object> candidateStat = new HashMap<>();
            candidateStat.put("id", candidate.getId());
            candidateStat.put("name", candidate.getName());
            candidateStat.put("party", candidate.getParty());
            candidateStat.put("voteCount", results.getVoteCount(candidate.getId()));
            candidateStat.put("votePercentage", results.getVotePercentages().getOrDefault(candidate.getId(), 0.0));
            candidateStats.add(candidateStat);
        }
        statistics.put("candidateStats", candidateStats);
        
        // Winner information
        Candidate winner = results.getWinningCandidate();
        if (winner != null) {
            Map<String, Object> winnerInfo = new HashMap<>();
            winnerInfo.put("id", winner.getId());
            winnerInfo.put("name", winner.getName());
            winnerInfo.put("party", winner.getParty());
            winnerInfo.put("voteCount", results.getVoteCount(winner.getId()));
            winnerInfo.put("votePercentage", results.getVotePercentages().getOrDefault(winner.getId(), 0.0));
            statistics.put("winner", winnerInfo);
        }
        
        // Total votes
        statistics.put("totalVotes", results.getTotalVotes());
        
        // Results generation time
        statistics.put("resultsGeneratedAt", results.getGeneratedAt());
        
        return statistics;
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
        
        // Store vote in database (without block hash since it's not mined yet)
        voteRepository.saveVote(voteTransaction, null);
        
        // Mine pending transactions if there are enough
        if (blockchain.getPendingTransactions().size() >= 5) {
            // Get the current chain size before mining
            int chainSizeBefore = blockchain.getChain().size();
            
            // Mine pending transactions
            blockchain.minePendingTransactions();
            
            // Check if a new block was added
            if (blockchain.getChain().size() > chainSizeBefore) {
                // Get the newly added block
                Block newBlock = blockchain.getChain().get(blockchain.getChain().size() - 1);
                
                // Update vote records with block hash
                for (Transaction tx : newBlock.getTransactions()) {
                    if (tx instanceof VoteTransaction) {
                        VoteTransaction voteTx = (VoteTransaction) tx;
                        voteRepository.saveVote(voteTx, newBlock.getHash());
                    }
                }
            }
            
            // Save blockchain after mining
            saveBlockchainState();
        }

        
        // Log the vote
        auditLog.logEvent("Vote cast by voter: " + voter.getId() + " in election: " + electionId);
        
        // Generate and save updated election results
        ElectionResults results = generateElectionResults(electionId);
        resultsRepository.saveElectionResults(results);
        
        // Save the blockchain state after adding the transaction
        saveBlockchainState();
    }
    public ElectionResults getElectionResults(String electionId) {
        // Try to get results from database first
        Optional<ElectionResults> storedResults = resultsRepository.getElectionResults(electionId, electionManager);
        
        if (storedResults.isPresent()) {
            return storedResults.get();
        }
        
        // If no stored results, generate them from blockchain
        ElectionResults results = generateElectionResults(electionId);
        
        // Save the generated results
        resultsRepository.saveElectionResults(results);
        
        return results;
    }
    
    private ElectionResults generateElectionResults(String electionId) {
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
    
    // Results and Blockchain Management
//    public ElectionResults getElectionResults(String electionId) {
//        // Check if election exists
//        Optional<Election> electionOpt = electionManager.getElection(electionId);
//        
//        if (electionOpt.isEmpty()) {
//            throw new VotingException("Election not found: " + electionId);
//        }
//        
//        Election election = electionOpt.get();
//        
//        // Create election results
//        ElectionResults results = new ElectionResults(election);
//        
//        // Count votes from blockchain
//        List<Block> blocks = blockchain.getChain();
//        
//        for (Block block : blocks) {
//            for (Transaction transaction : block.getTransactions()) {
//                if (transaction instanceof VoteTransaction) {
//                    VoteTransaction voteTransaction = (VoteTransaction) transaction;
//                    
//                    if (voteTransaction.getElectionId().equals(electionId)) {
//                        results.countVote(voteTransaction.getCandidateId());
//                    }
//                }
//            }
//        }
//        
//        // Also count votes from pending transactions
//        for (Transaction transaction : blockchain.getPendingTransactions()) {
//            if (transaction instanceof VoteTransaction) {
//                VoteTransaction voteTransaction = (VoteTransaction) transaction;
//                
//                if (voteTransaction.getElectionId().equals(electionId)) {
//                    results.countVote(voteTransaction.getCandidateId());
//                }
//            }
//        }
//        
//        auditLog.logEvent("Election results generated for: " + electionId);
//        return results;
//    }
    
    public void mineBlockchain() {
        // Get the current chain size before mining
        int chainSizeBefore = blockchain.getChain().size();
        
        // Mine pending transactions
        blockchain.minePendingTransactions();
        
        // Check if a new block was added
        if (blockchain.getChain().size() > chainSizeBefore) {
            // Get the newly added block
            Block newBlock = blockchain.getChain().get(blockchain.getChain().size() - 1);
            
            // Update vote records with block hash
            for (Transaction tx : newBlock.getTransactions()) {
                if (tx instanceof VoteTransaction) {
                    VoteTransaction voteTx = (VoteTransaction) tx;
                    voteRepository.saveVote(voteTx, newBlock.getHash());
                }
            }
        }
        
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
    
    public List<VoteTransaction> getVotesForElection(String electionId) {
        return voteRepository.getVotesForElection(electionId);
    }
    
    public int getVoteCountForCandidate(String electionId, String candidateId) {
        return voteRepository.getVoteCountForCandidate(electionId, candidateId);
    }
}
