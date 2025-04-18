package api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import user.User;
import user.UserService;

import java.util.Optional;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private UserService userService;

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello, this is a test endpoint!");
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> findUser(@PathVariable String username) {
        try {
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return ResponseEntity.ok("User found: " + user.getUsername() + ", Email: " + user.getEmail());
            } else {
                return ResponseEntity.ok("User not found with username: " + username);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/db-connection")
    public ResponseEntity<String> testDbConnection() {
        try {
            long countUsers = userService.countUsers();
            System.out.println(countUsers);
            return ResponseEntity.ok("Database connection successful!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Database connection failed: " + e.getMessage());
        }
    }

}
