package api.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {
    
    @GetMapping("/")
    public String welcome() {
        return "Welcome to the Blockchain Voting System API. Please use the appropriate endpoints to interact with the system.";
    }
}
