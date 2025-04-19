package api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import user.CryptoUtils;
import user.User;
import user.UserService;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private UserService userService;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ... other methods ...

    @GetMapping("/create-admin")
    public ResponseEntity<?> createAdminUser() {
        try {
            // Check if admin already exists
            if (userService.findByUsername("admin").isPresent()) {
                return ResponseEntity.ok("Admin user already exists");
            }
            
            // Create admin user
            User admin = userService.registerUser("admin", "admin@example.com", "password");
            
            // Set admin role and verify
            admin.setRole("ADMIN");
            admin.setVerified(true);
            userService.updateUser(admin);
            
            return ResponseEntity.ok("Admin user created successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating admin user: " + e.getMessage());
        }
    }
}
