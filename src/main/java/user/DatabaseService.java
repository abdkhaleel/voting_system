package user;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private static HikariDataSource dataSource;
    
    static {
        try {
            initializeDataSource();
        } catch (Exception e) {
            System.err.println("Error initializing database connection pool: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    private static void initializeDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/voting_system");
        config.setUsername("khaleel"); // Change as needed
        config.setPassword("abdulkhaleel@2004"); // Change as needed
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(10000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        dataSource = new HikariDataSource(config);
    }
    
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    public static void executeUpdate(String sql, Object... params) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = connection.prepareStatement(sql);
            
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
            closeConnection(connection);
        }
    }
    
    public static <T> List<T> executeQuery(String sql, ResultSetMapper<T> mapper, Object... params) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<T> results = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = connection.prepareStatement(sql);
            
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                results.add(mapper.map(resultSet));
            }
            
            return results;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            closeConnection(connection);
        }
    }
    
    public interface ResultSetMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }
}
