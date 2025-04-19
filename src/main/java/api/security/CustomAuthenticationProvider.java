package api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import user.User;
import user.UserService;

import java.util.Collections;
import java.util.Optional;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserService userService;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        
        System.out.println("CustomAuthenticationProvider: Authenticating " + username);
        
        try {
            Optional<User> userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                System.out.println("User not found: " + username);
                throw new BadCredentialsException("Invalid username or password");
            }
            
            User user = userOpt.get();
            System.out.println("User found: " + user.getUsername());
            System.out.println("Stored password hash: " + user.getPasswordHash());
            
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                System.out.println("Password matches for user: " + username);
                
                // Update last login time
                user.updateLastLogin();
                userService.updateUser(user);
                
                return new UsernamePasswordAuthenticationToken(
                    username, 
                    user.getPasswordHash(), 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                );
            } else {
                System.out.println("Password does not match for user: " + username);
                throw new BadCredentialsException("Invalid username or password");
            }
        } catch (Exception e) {
            System.err.println("Authentication error in provider: " + e.getMessage());
            e.printStackTrace();
            throw new BadCredentialsException("Authentication error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
