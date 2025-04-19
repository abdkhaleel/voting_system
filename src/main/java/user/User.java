package user;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.UUID;

public class User {
    private final String id;
    private String username;
    private String email;
    private String passwordHash;
    private final KeyPair keyPair;
    private boolean verified;
    private String role;
    private final LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    
 // Constructor for creating a new user
    public User(String username, String email, String passwordHash, KeyPair keyPair) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.keyPair = keyPair;
        this.verified = false;
        this.role = "USER";
        this.createdAt = LocalDateTime.now();
        this.lastLogin = LocalDateTime.now();
    }

    // Constructor for loading a user from the database
    public User(String id, String username, String email, String passwordHash, KeyPair keyPair, 
                boolean verified, String role, LocalDateTime createdAt, LocalDateTime lastLogin) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.keyPair = keyPair;
        this.verified = verified;
        this.role = role;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    
    // Getters and setters
    
    public String getId() {
        return id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
    
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
    
    public boolean isVerified() {
        return verified;
    }
    
    public void setVerified(boolean verified) {
        this.verified = verified;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }
}
