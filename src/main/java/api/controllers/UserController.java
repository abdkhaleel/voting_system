package api.controllers;

import api.dto.UserDTO;
import api.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import user.User;
import user.UserService;
import user.VerificationService;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    private final VerificationService verificationService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    
    @Autowired
    public UserController(UserService userService, VerificationService verificationService,
                         AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.userService = userService;
        this.verificationService = verificationService;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserDTO userDTO) {
        try {
            User user = userService.registerUser(userDTO.getUsername(), userDTO.getEmail(), userDTO.getPassword());
            
            // Generate verification token
            String token = verificationService.generateVerificationToken(user.getId());
            
            // In a real application, you would send this token via email
            // For simplicity, we'll just return it in the response
            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("verificationToken", token);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody UserDTO userDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userDTO.getUsername(), userDTO.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            
            Map<String, String> response = new HashMap<>();
            response.put("token", jwt);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    @GetMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestParam String token) {
        boolean verified = verificationService.verifyUser(token);
        
        Map<String, String> response = new HashMap<>();
        if (verified) {
            response.put("message", "User verified successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Invalid or expired verification token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            Optional<User> userOpt = userService.findByUsername(authentication.getName());
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                UserDTO userDTO = new UserDTO();
                userDTO.setUsername(user.getUsername());
                userDTO.setEmail(user.getEmail());
                // Don't include sensitive information like password
                
                return ResponseEntity.ok(userDTO);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
