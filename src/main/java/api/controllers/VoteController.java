package api.controllers;

import api.dto.ApiResponse;
import api.dto.VoteDTO;
import exception.VotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import user.DatabaseService;
import user.User;
import user.UserService;
import voting.Election;
import voting.VotingSystem;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/votes")
public class VoteController {

    private final VotingSystem votingSystem;
    private final UserService userService;

    @Autowired
    public VoteController(DatabaseService databaseService, UserService userService) {
        this.votingSystem = new VotingSystem(databaseService);
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> castVote(@RequestBody VoteDTO voteDTO, Principal principal) {
        try {
            // Get the current user
            Optional<User> userOpt = userService.findByUsername(principal.getName());
            System.out.println(principal.getName());            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "User not found"));
            }
            
            User user = userOpt.get();
            // Get the election
            Optional<Election> electionOpt = votingSystem.getElection(voteDTO.getElectionId());
            
            if (electionOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "Election not found"));
            }
            
            // Cast the vote
            votingSystem.castVote(voteDTO.getElectionId(), voteDTO.getCandidateId(), user);
            
            return ResponseEntity.ok(new ApiResponse(true, "Vote cast successfully"));
        } catch (VotingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "An error occurred while casting vote: " + e.getMessage()));
        }
    }

    @GetMapping("/check/{electionId}")
    public ResponseEntity<?> checkVoteStatus(@PathVariable String electionId, Principal principal) {
        try {
            // Get the current user
            Optional<User> userOpt = userService.findByUsername(principal.getName());
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "User not found"));
            }
            
            User user = userOpt.get();
            
            // Get the election
            Optional<Election> electionOpt = votingSystem.getElection(electionId);
            
            if (electionOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse(false, "Election not found"));
            }
            
            boolean hasVoted = votingSystem.hasVoterVoted(electionId, user.getId());
            boolean isEligible = votingSystem.isVoterEligible(electionId, user.getId());
            
            VoteStatus status = new VoteStatus(isEligible, hasVoted);
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "An error occurred while checking vote status: " + e.getMessage()));
        }
    }
    
    // Helper class for vote status response
    public static class VoteStatus {
        private final boolean eligible;
        private final boolean hasVoted;
        
        public VoteStatus(boolean eligible, boolean hasVoted) {
            this.eligible = eligible;
            this.hasVoted = hasVoted;
        }
        
        public boolean isEligible() {
            return eligible;
        }
        
        public boolean isHasVoted() {
            return hasVoted;
        }
    }
}
