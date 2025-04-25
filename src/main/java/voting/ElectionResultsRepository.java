package voting;

import exception.VotingException;
import user.DatabaseService;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ElectionResultsRepository {
    private final DatabaseService dbService;
    private final AuditLog auditLog;

    public ElectionResultsRepository(DatabaseService dbService, AuditLog auditLog) {
        this.dbService = dbService;
        this.auditLog = auditLog;
        initializeTables();
    }

    private void initializeTables() {
        // Create table for election results
        String resultsTableSql = "CREATE TABLE IF NOT EXISTS election_results (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "election_id VARCHAR(36) NOT NULL, " +
                "generated_at TIMESTAMP NOT NULL, " +
                "total_votes INT NOT NULL, " +
                "FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE" +
                ")";

        // Create table for candidate vote counts
        String voteCountsTableSql = "CREATE TABLE IF NOT EXISTS candidate_vote_counts (" +
                "result_id VARCHAR(36) NOT NULL, " +
                "candidate_id VARCHAR(36) NOT NULL, " +
                "vote_count INT NOT NULL, " +
                "vote_percentage DOUBLE NOT NULL, " +
                "PRIMARY KEY (result_id, candidate_id), " +
                "FOREIGN KEY (result_id) REFERENCES election_results(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE" +
                ")";

        try (Connection conn = dbService.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(resultsTableSql)) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(voteCountsTableSql)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new VotingException("Failed to initialize election results tables: " + e.getMessage());
        }
    }

    public void saveElectionResults(ElectionResults results) {
        String resultId = java.util.UUID.randomUUID().toString();
        String electionId = results.getElection().getId();
        LocalDateTime generatedAt = results.getGeneratedAt();
        int totalVotes = results.getTotalVotes();
        Map<String, Integer> voteCounts = results.getVoteCounts();
        Map<String, Double> votePercentages = results.getVotePercentages();

        // Insert the election result
        String insertResultSql = "INSERT INTO election_results (id, election_id, generated_at, total_votes) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE generated_at = ?, total_votes = ?";

        // Insert vote counts for each candidate
        String insertVoteCountSql = "INSERT INTO candidate_vote_counts (result_id, candidate_id, vote_count, vote_percentage) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE vote_count = ?, vote_percentage = ?";

        try (Connection conn = dbService.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Insert or update the election result
                try (PreparedStatement stmt = conn.prepareStatement(insertResultSql)) {
                    stmt.setString(1, resultId);
                    stmt.setString(2, electionId);
                    stmt.setTimestamp(3, Timestamp.valueOf(generatedAt));
                    stmt.setInt(4, totalVotes);
                    stmt.setTimestamp(5, Timestamp.valueOf(generatedAt));
                    stmt.setInt(6, totalVotes);
                    stmt.executeUpdate();
                }

                // Delete existing vote counts for this result
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM candidate_vote_counts WHERE result_id = ?")) {
                    stmt.setString(1, resultId);
                    stmt.executeUpdate();
                }

                // Insert vote counts for each candidate
                try (PreparedStatement stmt = conn.prepareStatement(insertVoteCountSql)) {
                    for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
                        String candidateId = entry.getKey();
                        int voteCount = entry.getValue();
                        double percentage = votePercentages.getOrDefault(candidateId, 0.0);

                        stmt.setString(1, resultId);
                        stmt.setString(2, candidateId);
                        stmt.setInt(3, voteCount);
                        stmt.setDouble(4, percentage);
                        stmt.setInt(5, voteCount);
                        stmt.setDouble(6, percentage);
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                conn.commit();
                auditLog.logEvent("Election results saved to database for election: " + electionId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new VotingException("Failed to save election results: " + e.getMessage());
        }
    }

    public Optional<ElectionResults> getElectionResults(String electionId, ElectionManager electionManager) {
        String selectResultSql = "SELECT * FROM election_results WHERE election_id = ? ORDER BY generated_at DESC LIMIT 1";
        String selectVoteCountsSql = "SELECT * FROM candidate_vote_counts WHERE result_id = ?";

        try (Connection conn = dbService.getConnection()) {
            // Get the election
            Optional<Election> electionOpt = electionManager.getElection(electionId);
            if (electionOpt.isEmpty()) {
                return Optional.empty();
            }
            Election election = electionOpt.get();

            // Get the most recent result
            String resultId = null;
            LocalDateTime generatedAt = null;
            
            try (PreparedStatement stmt = conn.prepareStatement(selectResultSql)) {
                stmt.setString(1, electionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        resultId = rs.getString("id");
                        generatedAt = rs.getTimestamp("generated_at").toLocalDateTime();
                    } else {
                        return Optional.empty();
                    }
                }
            }

            // Create the results object
            ElectionResults results = new ElectionResults(election);
            
            // Set the generated timestamp using reflection
            try {
                java.lang.reflect.Field generatedAtField = ElectionResults.class.getDeclaredField("generatedAt");
                generatedAtField.setAccessible(true);
                generatedAtField.set(results, generatedAt);
            } catch (Exception e) {
                throw new SQLException("Failed to set result generation time: " + e.getMessage());
            }

            // Get the vote counts
            try (PreparedStatement stmt = conn.prepareStatement(selectVoteCountsSql)) {
                stmt.setString(1, resultId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String candidateId = rs.getString("candidate_id");
                        int voteCount = rs.getInt("vote_count");
                        
                        // Add votes to the results
                        for (int i = 0; i < voteCount; i++) {
                            results.countVote(candidateId);
                        }
                    }
                }
            }

            return Optional.of(results);
        } catch (SQLException e) {
            throw new VotingException("Failed to retrieve election results: " + e.getMessage());
        }
    }
}