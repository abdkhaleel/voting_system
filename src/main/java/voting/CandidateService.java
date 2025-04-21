package voting;

import user.DatabaseService;
import exception.VotingException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CandidateService {
    private final DatabaseService dbService;
    
    public CandidateService(DatabaseService dbService) {
        this.dbService = dbService;
        initializeTable();
    }
    
    private void initializeTable() {
        String sql = "CREATE TABLE IF NOT EXISTS candidates (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "election_id VARCHAR(36) NOT NULL, " +
                "name VARCHAR(100) NOT NULL, " +
                "party VARCHAR(100), " +
                "position VARCHAR(100), " +
                "biography TEXT, " +
                "image_url VARCHAR(255), " +
                "FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE" +
                ")";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new VotingException("Failed to initialize candidates table: " + e.getMessage());
        }
    }
    
    public Candidate createCandidate(String electionId, String name, String party, String position) {
        Candidate candidate = new Candidate(name, party, position);
        
        String sql = "INSERT INTO candidates (id, election_id, name, party, position) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, candidate.getId());
            stmt.setString(2, electionId);
            stmt.setString(3, candidate.getName());
            stmt.setString(4, candidate.getParty());
            stmt.setString(5, candidate.getPosition());
            stmt.executeUpdate();
            return candidate;
        } catch (SQLException e) {
            throw new VotingException("Failed to create candidate: " + e.getMessage());
        }
    }
    
    public void updateCandidate(Candidate candidate, String electionId) {
        String sql = "UPDATE candidates SET name = ?, party = ?, position = ?, biography = ?, image_url = ? " +
                "WHERE id = ? AND election_id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, candidate.getName());
            stmt.setString(2, candidate.getParty());
            stmt.setString(3, candidate.getPosition());
            stmt.setString(4, candidate.getBiography());
            stmt.setString(5, candidate.getImageUrl());
            stmt.setString(6, candidate.getId());
            stmt.setString(7, electionId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new VotingException("Candidate not found or not part of the specified election");
            }
        } catch (SQLException e) {
            throw new VotingException("Failed to update candidate: " + e.getMessage());
        }
    }
    
    public void deleteCandidate(String candidateId, String electionId) {
        String sql = "DELETE FROM candidates WHERE id = ? AND election_id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, candidateId);
            stmt.setString(2, electionId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new VotingException("Candidate not found or not part of the specified election");
            }
        } catch (SQLException e) {
            throw new VotingException("Failed to delete candidate: " + e.getMessage());
        }
    }
    
    public Optional<Candidate> getCandidate(String candidateId) {
        String sql = "SELECT * FROM candidates WHERE id = ?";
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, candidateId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCandidate(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new VotingException("Failed to get candidate: " + e.getMessage());
        }
    }
    
    public List<Candidate> getCandidatesForElection(String electionId) {
        String sql = "SELECT * FROM candidates WHERE election_id = ?";
        List<Candidate> candidates = new ArrayList<>();
        
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    candidates.add(mapResultSetToCandidate(rs));
                }
            }
            return candidates;
        } catch (SQLException e) {
            throw new VotingException("Failed to get candidates for election: " + e.getMessage());
        }
    }
    
    private Candidate mapResultSetToCandidate(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        String party = rs.getString("party");
        String position = rs.getString("position");
        
        Candidate candidate = new Candidate(name, party, position);
        // Use reflection to set the id field or modify your Candidate class to allow id setting
        try {
            java.lang.reflect.Field idField = Candidate.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(candidate, rs.getString("id"));
        } catch (Exception e) {
            throw new SQLException("Failed to set candidate ID: " + e.getMessage());
        }
        
        candidate.setBiography(rs.getString("biography"));
        candidate.setImageUrl(rs.getString("image_url"));
        
        return candidate;
    }
}
