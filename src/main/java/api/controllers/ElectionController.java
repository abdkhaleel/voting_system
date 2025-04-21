package api.controllers;

import api.dto.ApiResponse;
import api.dto.CandidateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import user.DatabaseService;
import user.User;
import user.UserService;
import voting.*;

import java.security.Principal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/elections")
public class ElectionController {

    private final VotingSystem votingSystem;
    private final UserService userService;

    @Autowired
    public ElectionController(DatabaseService databaseService, UserService userService) {
        this.votingSystem = new VotingSystem(databaseService);
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<Election>> getAllElections() {
        List<Election> elections = votingSystem.getAllElections();
        return ResponseEntity.ok(elections);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Election>> getActiveElections() {
        List<Election> activeElections = votingSystem.getActiveElections();
        return ResponseEntity.ok(activeElections);
    }

    @GetMapping("/{electionId}")
    public ResponseEntity<?> getElection(@PathVariable String electionId) {
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isPresent()) {
            return ResponseEntity.ok(electionOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createElection(@RequestBody Election electionRequest) {
        Election election = votingSystem.createElection(
                electionRequest.getTitle(),
                electionRequest.getDescription(),
                electionRequest.getStartDate(),
                electionRequest.getEndDate()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(election);
    }

    @PutMapping("/{electionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateElection(
            @PathVariable String electionId,
            @RequestBody Election electionRequest) {
        
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
        
        Election election = electionOpt.get();
        election.setTitle(electionRequest.getTitle());
        election.setDescription(electionRequest.getDescription());
        election.setStartDate(electionRequest.getStartDate());
        election.setEndDate(electionRequest.getEndDate());
        
        votingSystem.getElectionManager().updateElection(election);
        
        return ResponseEntity.ok(election);
    }

    @DeleteMapping("/{electionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteElection(@PathVariable String electionId) {
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
        
        votingSystem.getElectionManager().deleteElection(electionId);
        
        return ResponseEntity.ok(new ApiResponse(true, "Election deleted successfully"));
    }

    @PostMapping("/{electionId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> activateElection(@PathVariable String electionId) {
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
        
        votingSystem.activateElection(electionId);
        
        return ResponseEntity.ok(new ApiResponse(true, "Election activated successfully"));
    }

    @PostMapping("/{electionId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deactivateElection(@PathVariable String electionId) {
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
        
        votingSystem.deactivateElection(electionId);
        
        return ResponseEntity.ok(new ApiResponse(true, "Election deactivated successfully"));
    }

    @GetMapping("/{electionId}/candidates")
    public ResponseEntity<?> getCandidates(@PathVariable String electionId) {
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
        
        List<Candidate> candidates = votingSystem.getCandidatesForElection(electionId);
        
        return ResponseEntity.ok(candidates);
    }

    @PostMapping("/{electionId}/candidates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> addCandidate(
            @PathVariable String electionId,
            @RequestBody CandidateDTO candidateDTO) {
        
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
        
        votingSystem.addCandidateToElection(
                electionId,
                candidateDTO.getName(),
                candidateDTO.getParty(),
                candidateDTO.getPosition()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "Candidate added successfully"));
    }

    @DeleteMapping("/{electionId}/candidates/{candidateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> removeCandidate(
            @PathVariable String electionId,
            @PathVariable String candidateId) {
        
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
        
        Optional<Candidate> candidateOpt = votingSystem.getCandidate(candidateId);
        
        if (candidateOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Candidate not found"));
        }
        
        votingSystem.removeCandidate(candidateId, electionId);
        
        return ResponseEntity.ok(new ApiResponse(true, "Candidate removed successfully"));
    }

    @GetMapping("/{electionId}/results")
    public ResponseEntity<?> getElectionResults(@PathVariable String electionId) {
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
        
        ElectionResults results = votingSystem.getElectionResults(electionId);
        
        return ResponseEntity.ok(results);
    }

    @PostMapping("/{electionId}/voters/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> addEligibleVoter(
            @PathVariable String electionId,
            @PathVariable String userId) throws SQLException {
        
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
        
        Optional<User> userOpt = userService.findById(userId);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "User not found"));
        }
        
        votingSystem.addEligibleVoter(electionId, userId);
        
        return ResponseEntity.ok(new ApiResponse(true, "Voter added to eligible list successfully"));
    }

    @DeleteMapping("/{electionId}/voters/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> removeEligibleVoter(
            @PathVariable String electionId,
            @PathVariable String userId) throws SQLException {
        
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
        
        Optional<User> userOpt = userService.findById(userId);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "User not found"));
        }
        
        votingSystem.removeEligibleVoter(electionId, userId);
        
        return ResponseEntity.ok(new ApiResponse(true, "Voter removed from eligible list successfully"));
    }
    
    @GetMapping("/{electionId}/eligibility")
    public ResponseEntity<?> checkEligibility(
            @PathVariable String electionId,
            Principal principal) throws SQLException {
        
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
        
        Optional<User> userOpt = userService.findByUsername(principal.getName());
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "User not found"));
        }
        
        User user = userOpt.get();
        boolean isEligible = votingSystem.isVoterEligible(electionId, user.getId());
        boolean hasVoted = votingSystem.hasVoterVoted(electionId, user.getId());
        
        return ResponseEntity.ok(new EligibilityStatus(isEligible, hasVoted));
    }
    
    @GetMapping("/{electionId}/blockchain-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getBlockchainStatus(@PathVariable String electionId) {
        Optional<Election> electionOpt = votingSystem.getElection(electionId);
        
        if (electionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Election not found"));
        }
        
        boolean isValid = votingSystem.validateBlockchain();
        int pendingTransactions = votingSystem.getBlockchain().getPendingTransactions().size();
        int blockCount = votingSystem.getBlockchain().getChain().size();
        
        return ResponseEntity.ok(new BlockchainStatus(isValid, pendingTransactions, blockCount));
    }
    
    @PostMapping("/mine-blockchain")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> mineBlockchain() {
        votingSystem.mineBlockchain();
        return ResponseEntity.ok(new ApiResponse(true, "Blockchain mined successfully"));
    }
    
    // Helper classes for responses
    public static class EligibilityStatus {
        private final boolean eligible;
        private final boolean hasVoted;
        
        public EligibilityStatus(boolean eligible, boolean hasVoted) {
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
    
    public static class BlockchainStatus {
        private final boolean valid;
        private final int pendingTransactions;
        private final int blockCount;
        
        public BlockchainStatus(boolean valid, int pendingTransactions, int blockCount) {
            this.valid = valid;
            this.pendingTransactions = pendingTransactions;
            this.blockCount = blockCount;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public int getPendingTransactions() {
            return pendingTransactions;
        }
        
        public int getBlockCount() {
            return blockCount;
        }
    }
}
