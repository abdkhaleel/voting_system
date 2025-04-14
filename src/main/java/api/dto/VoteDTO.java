package api.dto;

import javax.validation.constraints.NotBlank;

public class VoteDTO {
    
    @NotBlank(message = "Election ID is required")
    private String electionId;
    
    @NotBlank(message = "Candidate ID is required")
    private String candidateId;
    
    // Getters and setters
    public String getElectionId() {
        return electionId;
    }
    
    public void setElectionId(String electionId) {
        this.electionId = electionId;
    }
    
    public String getCandidateId() {
        return candidateId;
    }
    
    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }
}
