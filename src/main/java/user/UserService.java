package user;

import java.security.KeyPair;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import exception.UserException;

public class UserService {
    
    public User registerUser(String username, String email, String password) {
        try {
            // Check if username or email already exists
            if (findByUsername(username).isPresent()) {
                throw new UserException("Username already exists");
            }
            
            if (findByEmail(email).isPresent()) {
                throw new UserException("Email already exists");
            }
            
            // Generate salt and hash password
            String salt = CryptoUtils.generateSalt();
            String passwordHash = CryptoUtils.hashPassword(password, salt);
            
            // Generate key pair
            KeyPair keyPair = CryptoUtils.generateKeyPair();
            
            // Create user
            User user = new User(username, email, passwordHash, salt, keyPair);
            
            // Save user to database
            saveUser(user);
            
            return user;
        } catch (SQLException e) {
            throw new UserException("Error registering user: " + e.getMessage(), e);
        }
    }
    
    public Optional<User> authenticateUser(String username, String password) {
        try {
            Optional<User> userOpt = findByUsername(username);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String hashedPassword = CryptoUtils.hashPassword(password, user.getSalt());
                
                if (hashedPassword.equals(user.getPasswordHash())) {
                    user.updateLastLogin();
                    updateUser(user);
                    return Optional.of(user);
                }
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            throw new UserException("Error authenticating user: " + e.getMessage(), e);
        }
    }
    
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<User> users = DatabaseService.executeQuery(sql, this::mapResultSetToUser, username);
        
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        List<User> users = DatabaseService.executeQuery(sql, this::mapResultSetToUser, email);
        
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    public Optional<User> findById(String id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = DatabaseService.executeQuery(sql, this::mapResultSetToUser, id);
        
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    private void saveUser(User user) throws SQLException {
        String sql = "INSERT INTO users (id, username, email, password_hash, salt, public_key, private_key, verified, role, created_at, last_login) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        DatabaseService.executeUpdate(sql, 
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getSalt(),
            user.getPublicKey().getEncoded(),
            user.getPrivateKey().getEncoded(),
            user.isVerified(),
            user.getRole(),
            user.getCreatedAt(),
            user.getLastLogin()
        );
    }
    
    private void updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, email = ?, password_hash = ?, salt = ?, " +
                     "public_key = ?, private_key = ?, verified = ?, role = ?, last_login = ? " +
                     "WHERE id = ?";
        
        DatabaseService.executeUpdate(sql, 
            user.getUsername(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getSalt(),
            user.getPublicKey().getEncoded(),
            user.getPrivateKey().getEncoded(),
            user.isVerified(),
            user.getRole(),
            user.getLastLogin(),
            user.getId()
        );
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        // This would need to be implemented based on your actual database schema
        // This is a simplified example
        try {
            String id = rs.getString("id");
            String username = rs.getString("username");
            String email = rs.getString("email");
            String passwordHash = rs.getString("password_hash");
            String salt = rs.getString("salt");
            byte[] publicKeyBytes = rs.getBytes("public_key");
            byte[] privateKeyBytes = rs.getBytes("private_key");
            
            // You would need to reconstruct the KeyPair from the stored bytes
            // This is simplified and would need proper implementation
            KeyPair keyPair = KeyPairUtils.fromEncodedKeys(publicKeyBytes, privateKeyBytes);
            
            User user = new User(username, email, passwordHash, salt, keyPair);
            // Set other properties from the result set
            
            return user;
        } catch (Exception e) {
            throw new SQLException("Error mapping result set to user", e);
        }
    }
    
}
