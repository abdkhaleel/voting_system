package user;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import exception.UserException;

public class VerificationService {
    
    private final UserService userService;
    private final Map<String, VerificationToken> tokens;
    
    public VerificationService(UserService userService) {
        this.userService = userService;
        this.tokens = new HashMap<>();
    }
    
    public String generateVerificationToken(String userId) {
        // Generate a random token
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Store the token with expiration time (24 hours)
        VerificationToken verificationToken = new VerificationToken(userId, token, LocalDateTime.now().plusHours(24));
        tokens.put(token, verificationToken);
        
        return token;
    }
    
    public boolean verifyUser(String token) {
        VerificationToken verificationToken = tokens.get(token);
        
        if (verificationToken == null) {
            return false;
        }
        
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokens.remove(token);
            return false;
        }
        
        try {
            Optional<User> userOpt = userService.findById(verificationToken.getUserId());
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setVerified(true);
                // Update user in database
                // userService.updateUser(user);
                tokens.remove(token);
                return true;
            }
            
            return false;
        } catch (SQLException e) {
            throw new UserException("Error verifying user: " + e.getMessage(), e);
        }
    }
    
    private static class VerificationToken {
        private final String userId;
        private final String token;
        private final LocalDateTime expiryDate;
        
        public VerificationToken(String userId, String token, LocalDateTime expiryDate) {
            this.userId = userId;
            this.token = token;
            this.expiryDate = expiryDate;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getToken() {
            return token;
        }
        
        public LocalDateTime getExpiryDate() {
            return expiryDate;
        }
    }
}
