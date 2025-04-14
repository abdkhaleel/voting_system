package voting;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Election implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
    private List<Candidate> candidates;
    private List<String> eligibleVoterIds;
    private List<String> votedVoterIds;
    
    public Election(String title, String description, LocalDateTime startDate, LocalDateTime endDate) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = false;
        this.candidates = new ArrayList<>();
        this.eligibleVoterIds = new ArrayList<>();
        this.votedVoterIds = new ArrayList<>();
    }
    
    public void addCandidate(Candidate candidate) {
        candidates.add(candidate);
    }
    
    public void removeCandidate(String candidateId) {
        candidates.removeIf(c -> c.getId().equals(candidateId));
    }
    
    public void addEligibleVoter(String voterId) {
        if (!eligibleVoterIds.contains(voterId)) {
            eligibleVoterIds.add(voterId);
        }
    }
    
    public void removeEligibleVoter(String voterId) {
        eligibleVoterIds.remove(voterId);
    }
    
    public boolean isVoterEligible(String voterId) {
        return eligibleVoterIds.contains(voterId);
    }
    
    public boolean hasVoterVoted(String voterId) {
        return votedVoterIds.contains(voterId);
    }
    
    public void markVoterAsVoted(String voterId) {
        if (!votedVoterIds.contains(voterId)) {
            votedVoterIds.add(voterId);
        }
    }
    
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return active && now.isAfter(startDate) && now.isBefore(endDate);
    }
    
    public void activate() {
        this.active = true;
    }
    
    public void deactivate() {
        this.active = false;
    }

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDateTime getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDateTime startDate) {
		this.startDate = startDate;
	}

	public LocalDateTime getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDateTime endDate) {
		this.endDate = endDate;
	}

	public List<Candidate> getCandidates() {
		return candidates;
	}

	public void setCandidates(List<Candidate> candidates) {
		this.candidates = candidates;
	}

	public List<String> getEligibleVoterIds() {
		return eligibleVoterIds;
	}

	public void setEligibleVoterIds(List<String> eligibleVoterIds) {
		this.eligibleVoterIds = eligibleVoterIds;
	}

	public List<String> getVotedVoterIds() {
		return votedVoterIds;
	}

	public void setVotedVoterIds(List<String> votedVoterIds) {
		this.votedVoterIds = votedVoterIds;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getId() {
		return id;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
    
}
