package voting;

import blockchain.VoteTransaction;
import exception.VotingException;
import user.DatabaseService;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VoteRepository {
    private final DatabaseService dbService;
    private final AuditLog auditLog;

    public VoteRepository(DatabaseService dbService, AuditLog auditLog) {
        this.dbService = dbService;
        this.auditLog = auditLog;
        initializeTable();
    }

    private void initializeTable() {
        String sql = "CREATE TABLE IF NOT EXISTS votes (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "election_id VARCHAR(36) NOT NULL, " +
                "candidate_id VARCHAR(36) NOT NULL, " +
                "voter_id VARCHAR(36) NOT NULL, " +
                "timestamp TIMESTAMP NOT NULL, " +
                "transaction_hash VARCHAR(64) NOT NULL, " +
                "block_hash VARCHAR(64), " +
                "FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE" +
                ")";

        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new VotingException("Failed to initialize votes table: " + e.getMessage());
        }
    }

    public void saveVote(VoteTransaction transaction, String blockHash) {
        String sql = "INSERT INTO votes (id, election_id, candidate_id, voter_id, timestamp, transaction_hash, block_hash) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, java.util.UUID.randomUUID().toString());
            stmt.setString(2, transaction.getElectionId());
            stmt.setString(3, transaction.getCandidateId());
            stmt.setString(4, transaction.getVoterId());
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(6, transaction.calculateHash());
            stmt.setString(7, blockHash);
            stmt.executeUpdate();

            auditLog.logEvent("Vote recorded in database for election: " + transaction.getElectionId());
        } catch (SQLException e) {
            throw new VotingException("Failed to save vote: " + e.getMessage());
        }
    }

    public List<VoteTransaction> getVotesForElection(String electionId) {
        String sql = "SELECT * FROM votes WHERE election_id = ?";
        List<VoteTransaction> votes = new ArrayList<>();

        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String voterId = rs.getString("voter_id");
                    String candidateId = rs.getString("candidate_id");
                    
                    VoteTransaction transaction = new VoteTransaction(voterId, electionId, candidateId);
                    votes.add(transaction);
                }
            }
            return votes;
        } catch (SQLException e) {
            throw new VotingException("Failed to get votes for election: " + e.getMessage());
        }
    }

    public int getVoteCountForCandidate(String electionId, String candidateId) {
        String sql = "SELECT COUNT(*) FROM votes WHERE election_id = ? AND candidate_id = ?";

        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, electionId);
            stmt.setString(2, candidateId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new VotingException("Failed to get vote count: " + e.getMessage());
        }
    }
}
