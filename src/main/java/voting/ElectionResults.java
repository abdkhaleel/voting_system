package voting;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ElectionResults implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Election election;
    private final Map<String, Integer> voteCounts;
    private final LocalDateTime generatedAt;
    
    public ElectionResults(Election election) {
        this.election = election;
        this.voteCounts = new HashMap<>();
        this.generatedAt = LocalDateTime.now();
        
        // Initialize vote counts for all candidates
        for (Candidate candidate : election.getCandidates()) {
            voteCounts.put(candidate.getId(), 0);
        }
    }
    
    public void countVote(String candidateId) {
        voteCounts.compute(candidateId, (id, count) -> count == null ? 1 : count + 1);
    }
    
    public int getVoteCount(String candidateId) {
        return voteCounts.getOrDefault(candidateId, 0);
    }
    
    public int getTotalVotes() {
        return voteCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    public Map<String, Double> getVotePercentages() {
        int totalVotes = getTotalVotes();
        Map<String, Double> percentages = new HashMap<>();
        
        if (totalVotes > 0) {
            for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / totalVotes;
                percentages.put(entry.getKey(), percentage);
            }
        }
        
        return percentages;
    }
    
    public String getWinningCandidateId() {
        return voteCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    public Candidate getWinningCandidate() {
        String winningId = getWinningCandidateId();
        
        if (winningId == null) {
            return null;
        }
        
        return election.getCandidates().stream()
                .filter(c -> c.getId().equals(winningId))
                .findFirst()
                .orElse(null);
    }
    
    // Getters
    public Election getElection() {
        return election;
    }
    
    public Map<String, Integer> getVoteCounts() {
        return new HashMap<>(voteCounts);
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
