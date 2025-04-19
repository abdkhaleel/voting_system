package api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.Base64;

@Component
public class JwtTokenProvider {

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private int jwtExpirationInMs;
    
    private Key key;
    
    @PostConstruct
    public void init() {
        // Use a secure key for HS512 algorithm
        // If jwtSecret is provided in properties, use it to generate a secure key
        if (jwtSecret != null && !jwtSecret.isEmpty()) {
            try {
                // Ensure the secret is at least 64 bytes (512 bits)
                byte[] keyBytes = jwtSecret.getBytes();
                if (keyBytes.length < 64) {
                    // Pad the key if it's too short
                    byte[] paddedKey = new byte[64];
                    System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
                    keyBytes = paddedKey;
                }
                key = Keys.hmacShaKeyFor(keyBytes);
            } catch (Exception e) {
                // Fallback to generating a secure key
                System.out.println("Using generated secure key for JWT signing");
                key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            }
        } else {
            // Generate a secure key if no secret is provided
            System.out.println("Using generated secure key for JWT signing");
            key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }
    }

    public String generateToken(Authentication authentication) {
        // Get username from the authentication object
        String username = authentication.getName();
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        }
        return false;
    }
}
