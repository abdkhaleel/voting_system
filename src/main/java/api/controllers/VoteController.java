package api.controllers;

import api.dto.VoteDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import user.User;
import user.UserService;
import voting.ElectionResults;
import voting.VotingSystem;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/votes")
public class VoteController {
    
    private final VotingSystem votingSystem;
    private final UserService userService;
    
    @Autowired
    public VoteController(VotingSystem votingSystem, UserService userService) {
        this.votingSystem = votingSystem;
        this.userService = userService;
    }
    
    @PostMapping
    public ResponseEntity<?> castVote(@Valid @RequestBody VoteDTO voteDTO, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            Optional<User> userOpt = userService.findByUsername(authentication.getName());
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            User voter = userOpt.get();
            
            votingSystem.castVote(voteDTO.getElectionId(), voteDTO.getCandidateId(), voter);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Vote cast successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/results/{electionId}")
    public ResponseEntity<?> getElectionResults(@PathVariable String electionId) {
        try {
            ElectionResults results = votingSystem.getElectionResults(electionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("electionId", electionId);
            response.put("totalVotes", results.getTotalVotes());
            response.put("voteCounts", results.getVoteCounts());
            response.put("percentages", results.getVotePercentages());
            
            if (results.getWinningCandidate() != null) {
                response.put("winner", results.getWinningCandidate().getName());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/verify/{electionId}")
    public ResponseEntity<?> verifyElectionResults(@PathVariable String electionId) {
        try {
            // Verify blockchain integrity
            boolean isValid = votingSystem.getBlockchain().isChainValid();
            
            Map<String, Object> response = new HashMap<>();
            response.put("electionId", electionId);
            response.put("blockchainValid", isValid);
            
            if (isValid) {
                response.put("message", "Election results are verified and blockchain integrity is confirmed");
            } else {
                response.put("message", "Blockchain integrity check failed. Results may be compromised");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
