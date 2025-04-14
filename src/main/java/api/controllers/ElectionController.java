package api.controllers;

import api.dto.CandidateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import voting.Candidate;
import voting.Election;
import voting.VotingSystem;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/elections")
public class ElectionController {
    
    private final VotingSystem votingSystem;
    
    @Autowired
    public ElectionController(VotingSystem votingSystem) {
        this.votingSystem = votingSystem;
    }
    
    @GetMapping
    public ResponseEntity<List<Election>> getAllElections() {
        List<Election> elections = votingSystem.getElectionManager().getAllElections();
        return ResponseEntity.ok(elections);
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<Election>> getActiveElections() {
        List<Election> activeElections = votingSystem.getElectionManager().getActiveElections();
        return ResponseEntity.ok(activeElections);
    }
    
    @GetMapping("/{electionId}")
    public ResponseEntity<?> getElection(@PathVariable String electionId) {
        Optional<Election> electionOpt = votingSystem.getElectionManager().getElection(electionId);
        
        if (electionOpt.isPresent()) {
            return ResponseEntity.ok(electionOpt.get());
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Election not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createElection(@Valid @RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String description = request.get("description");
            LocalDateTime startDate = LocalDateTime.parse(request.get("startDate"));
            LocalDateTime endDate = LocalDateTime.parse(request.get("endDate"));
            
            Election election = votingSystem.createElection(title, description, startDate, endDate);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(election);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/{electionId}/candidates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addCandidate(@PathVariable String electionId, @Valid @RequestBody CandidateDTO candidateDTO) {
        try {
            Candidate candidate = new Candidate(candidateDTO.getName(), candidateDTO.getParty(), candidateDTO.getPosition());
            
            if (candidateDTO.getBiography() != null) {
                candidate.setBiography(candidateDTO.getBiography());
            }
            
            if (candidateDTO.getImageUrl() != null) {
                candidate.setImageUrl(candidateDTO.getImageUrl());
            }
            
            votingSystem.addCandidateToElection(electionId, candidate);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(candidate);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/{electionId}/voters/{voterId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addEligibleVoter(@PathVariable String electionId, @PathVariable String voterId) {
        try {
            votingSystem.addEligibleVoter(electionId, voterId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Voter added to eligible voters list");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/{electionId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateElection(@PathVariable String electionId) {
        try {
            votingSystem.activateElection(electionId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Election activated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/{electionId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateElection(@PathVariable String electionId) {
        try {
            votingSystem.deactivateElection(electionId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Election deactivated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
