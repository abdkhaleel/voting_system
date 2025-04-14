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
    private String salt;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private boolean verified;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    
    public User(String username, String email, String passwordHash, String salt, KeyPair keyPair) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
        this.verified = false;
        this.role = "VOTER"; // Default role
        this.createdAt = LocalDateTime.now();
    }
    
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
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

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
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

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(LocalDateTime lastLogin) {
		this.lastLogin = lastLogin;
	}

	public String getId() {
		return id;
	}
    
}
