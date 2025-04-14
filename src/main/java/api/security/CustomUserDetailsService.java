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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            Optional<User> userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                throw new UsernameNotFoundException("User not found with username: " + username);
            }
            
            User user = userOpt.get();
            
            return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
            );
        } catch (SQLException e) {
            throw new UsernameNotFoundException("Error loading user: " + e.getMessage(), e);
        }
    }
}
