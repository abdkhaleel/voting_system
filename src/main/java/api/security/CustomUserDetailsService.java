package api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import user.User;
import user.UserService;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

 // In your CustomUserDetailsService
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            System.out.println("Loading user details for: " + username);
            Optional<User> userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                System.out.println("User not found: " + username);
                throw new UsernameNotFoundException("User not found with username: " + username);
            }
            
            User user = userOpt.get();
            System.out.println("User found: " + user.getUsername() + ", Role: " + user.getRole());
            System.out.println("Password hash from DB: " + user.getPasswordHash());
            
            // Check if the password hash looks like a BCrypt hash
            if (!user.getPasswordHash().startsWith("$2a$") && !user.getPasswordHash().startsWith("$2b$") && !user.getPasswordHash().startsWith("$2y$")) {
                System.out.println("WARNING: Password hash does not look like BCrypt format!");
            }
            
            return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
            );
        } catch (SQLException e) {
            System.err.println("Error loading user: " + e.getMessage());
            e.printStackTrace();
            throw new UsernameNotFoundException("Error loading user: " + e.getMessage(), e);
        }
    }

}
