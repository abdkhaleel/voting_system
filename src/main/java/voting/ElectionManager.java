            package voting;

import user.DatabaseService;
import exception.VotingException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ElectionManager {
    private final DatabaseService dbService;
    private final CandidateService candidateService;
    private final AuditLog auditLog;
    
    public ElectionManager(DatabaseService dbService, AuditLog auditLog) {
        this.dbService = dbService;
        this.auditLog = auditLog;
        this.candidateService = new CandidateService(dbService);
        initializeTable();
    }
    
    private void initializeTable() {
        String sql = "CREATE TABLE IF NOT EXISTS elections (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "title VARCHAR(100) NOT NULL, " +
                "description TEXT, " +
                "start_date TIMESTAMP NOT NULL, " +
                "end_date TIMESTAMP NOT NULL, " +
                "active BOOLEAN DEFAULT FALSE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new VotingException("Failed to initialize elections table: " + e.getMessage());
        }
    }
    
    public Election createElection(String title, String description, LocalDateTime startDate, LocalDateTime endDate) {
        Election election = new Election(title, description, startDate, endDate);
        
        String sql = "INSERT INTO elections (id, title, description, start_date, end_date, active) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, election.getId());
            stmt.setString(2, title);
            stmt.setString(3, description);
            stmt.setTimestamp(4, Timestamp.valueOf(startDate));
            stmt.setTimestamp(5, Timestamp.valueOf(endDate));
            stmt.setBoolean(6, false);
            stmt.executeUpdate();
            
            auditLog.logEvent("Election created: " + election.getId());
            return election;
        } catch (SQLException e) {
            throw new VotingException("Failed to create election: " + e.getMessage());
        }
    }
    
    public Optional<Election> getElection(String electionId) {
        String sql = "SELECT * FROM elections WHERE id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Election election = mapResultSetToElection(rs);
                    
                    // Load candidates
                    election.setCandidates(candidateService.getCandidatesForElection(electionId));
                    
                    // Load eligible voters
                    loadEligibleVoters(election);
                    
                    // Load voted voters
                    loadVotedVoters(election);
                    
                    return Optional.of(election);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new VotingException("Failed to get election: " + e.getMessage());
        }
    }
    
    public List<Election> getAllElections() {
        String sql = "SELECT * FROM elections ORDER BY start_date DESC";
        List<Election> elections = new ArrayList<>();
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Election election = mapResultSetToElection(rs);
                elections.add(election);
            }
            
            // Load related data for each election
            for (Election election : elections) {
                election.setCandidates(candidateService.getCandidatesForElection(election.getId()));
                loadEligibleVoters(election);
                loadVotedVoters(election);
            }
            
            return elections;
        } catch (SQLException e) {
            throw new VotingException("Failed to get all elections: " + e.getMessage());
        }
    }
    
    public List<Election> getActiveElections() {
        String sql = "SELECT * FROM elections WHERE active = TRUE AND start_date <= ? AND end_date >= ?";
        List<Election> elections = new ArrayList<>();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, now);
            stmt.setTimestamp(2, now);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Election election = mapResultSetToElection(rs);
                    elections.add(election);
                }
            }
            
            // Load related data for each election
            for (Election election : elections) {
                election.setCandidates(candidateService.getCandidatesForElection(election.getId()));
                loadEligibleVoters(election);
                loadVotedVoters(election);
            }
            
            return elections;
        } catch (SQLException e) {
            throw new VotingException("Failed to get active elections: " + e.getMessage());
        }
    }
    
    public void updateElection(Election election) {
        String sql = "UPDATE elections SET title = ?, description = ?, start_date = ?, end_date = ?, active = ? WHERE id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, election.getTitle());
            stmt.setString(2, election.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(election.getStartDate()));
            stmt.setTimestamp(4, Timestamp.valueOf(election.getEndDate()));
            stmt.setBoolean(5, election.isActive());
            stmt.setString(6, election.getId());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new VotingException("Election not found: " + election.getId());
            }
            
            // Update eligible voters
            updateEligibleVoters(election);
            
            // Update voted voters
            updateVotedVoters(election);
            
            auditLog.logEvent("Election updated: " + election.getId());
        } catch (SQLException e) {
            throw new VotingException("Failed to update election: " + e.getMessage());
        }
    }
    
    public void deleteElection(String electionId) {
        // First delete related records
        deleteEligibleVoters(electionId);
        deleteVotedVoters(electionId);
        
        // Then delete the election
        String sql = "DELETE FROM elections WHERE id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new VotingException("Election not found: " + electionId);
            }
            
            auditLog.logEvent("Election deleted: " + electionId);
        } catch (SQLException e) {
            throw new VotingException("Failed to delete election: " + e.getMessage());
        }
    }
    
    public void activateElection(String electionId) {
        String sql = "UPDATE elections SET active = TRUE WHERE id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new VotingException("Election not found: " + electionId);
            }
            
            auditLog.logEvent("Election activated: " + electionId);
        } catch (SQLException e) {
            throw new VotingException("Failed to activate election: " + e.getMessage());
        }
    }
    
    public void deactivateElection(String electionId) {
        String sql = "UPDATE elections SET active = FALSE WHERE id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
			int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new VotingException("Election not found: " + electionId);
            }
            
            auditLog.logEvent("Election deactivated: " + electionId);
        } catch (SQLException e) {
            throw new VotingException("Failed to deactivate election: " + e.getMessage());
        }
    }
    
    private Election mapResultSetToElection(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        LocalDateTime startDate = rs.getTimestamp("start_date").toLocalDateTime();
        LocalDateTime endDate = rs.getTimestamp("end_date").toLocalDateTime();
        boolean active = rs.getBoolean("active");
        System.out.println(active);
        Election election = new Election(title, description, startDate, endDate);
        
        if(active) {
        	System.out.println("Called this if");
        	election.activate();
        }
        System.out.println("map to election method check " + election.isActive());
        
        // Use reflection to set the id field
        try {
            java.lang.reflect.Field idField = Election.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(election, id);
        } catch (Exception e) {
            throw new SQLException("Failed to set election ID: " + e.getMessage());
        }
        
        election.setActive(active);
        return election;
    }
    
    // Methods for managing eligible voters
    private void loadEligibleVoters(Election election) {
        String sql = "SELECT user_id FROM eligible_voters WHERE election_id = ?";
        List<String> eligibleVoterIds = new ArrayList<>();
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, election.getId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    eligibleVoterIds.add(rs.getString("user_id"));
                }
            }
            
            election.setEligibleVoterIds(eligibleVoterIds);
        } catch (SQLException e) {
            throw new VotingException("Failed to load eligible voters: " + e.getMessage());
        }
    }
    
    private void updateEligibleVoters(Election election) {
        // First delete all existing eligible voters
        deleteEligibleVoters(election.getId());
        
        // Then insert the current list
        String sql = "INSERT INTO eligible_voters (election_id, user_id) VALUES (?, ?)";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (String voterId : election.getEligibleVoterIds()) {
                stmt.setString(1, election.getId());
                stmt.setString(2, voterId);
                stmt.addBatch();
            }
            
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new VotingException("Failed to update eligible voters: " + e.getMessage());
        }
    }
    
    private void deleteEligibleVoters(String electionId) {
        String sql = "DELETE FROM eligible_voters WHERE election_id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new VotingException("Failed to delete eligible voters: " + e.getMessage());
        }
    }
    
    // Methods for managing voted voters
    private void loadVotedVoters(Election election) {
        String sql = "SELECT user_id FROM voted_voters WHERE election_id = ?";
        List<String> votedVoterIds = new ArrayList<>();
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, election.getId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    votedVoterIds.add(rs.getString("user_id"));
                }
            }
            
            election.setVotedVoterIds(votedVoterIds);
        } catch (SQLException e) {
            throw new VotingException("Failed to load voted voters: " + e.getMessage());
        }
    }
    
    private void updateVotedVoters(Election election) {
        // First delete all existing voted voters
        deleteVotedVoters(election.getId());
        
        // Then insert the current list
        String sql = "INSERT INTO voted_voters (election_id, user_id) VALUES (?, ?)";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (String voterId : election.getVotedVoterIds()) {
                stmt.setString(1, election.getId());
                stmt.setString(2, voterId);
                stmt.addBatch();
            }
            
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new VotingException("Failed to update voted voters: " + e.getMessage());
        }
    }
    
    private void deleteVotedVoters(String electionId) {
        String sql = "DELETE FROM voted_voters WHERE election_id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new VotingException("Failed to delete voted voters: " + e.getMessage());
        }
    }
    
    public void addEligibleVoter(String electionId, String voterId) {
        String sql = "INSERT INTO eligible_voters (election_id, user_id) VALUES (?, ?)";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
            stmt.setString(2, voterId);
            stmt.executeUpdate();
            
            auditLog.logEvent("Voter " + voterId + " added as eligible for election " + electionId);
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                // Voter is already eligible, ignore
                return;
            }
            throw new VotingException("Failed to add eligible voter: " + e.getMessage());
        }
    }
    
    public void removeEligibleVoter(String electionId, String voterId) {
        String sql = "DELETE FROM eligible_voters WHERE election_id = ? AND user_id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
            stmt.setString(2, voterId);
            stmt.executeUpdate();
            
            auditLog.logEvent("Voter " + voterId + " removed from eligible list for election " + electionId);
        } catch (SQLException e) {
            throw new VotingException("Failed to remove eligible voter: " + e.getMessage());
        }
    }
    
    public void markVoterAsVoted(String electionId, String voterId) {
        String sql = "INSERT INTO voted_voters (election_id, user_id) VALUES (?, ?)";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
            stmt.setString(2, voterId);
            stmt.executeUpdate();
            
            auditLog.logEvent("Voter " + voterId + " marked as voted in election " + electionId);
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                throw new VotingException("Voter has already voted in this election");
            }
            throw new VotingException("Failed to mark voter as voted: " + e.getMessage());
        }
    }
    
    public boolean hasVoterVoted(String electionId, String voterId) {
        String sql = "SELECT 1 FROM voted_voters WHERE election_id = ? AND user_id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
            stmt.setString(2, voterId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new VotingException("Failed to check if voter has voted: " + e.getMessage());
        }
    }
    
    public boolean isVoterEligible(String electionId, String voterId) {
        String sql = "SELECT 1 FROM eligible_voters WHERE election_id = ? AND user_id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
            stmt.setString(2, voterId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new VotingException("Failed to check if voter is eligible: " + e.getMessage());
        }
    }
}
