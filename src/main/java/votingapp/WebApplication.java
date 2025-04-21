package votingapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import user.DatabaseService;
import user.UserService;
import user.VerificationService;
import voting.VotingSystem;

@SpringBootApplication
@ComponentScan(basePackages = {"api", "blockchain", "user", "voting", "config", "exception", "util", "votingapp"})
public class WebApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
    
    @Bean
    public DatabaseService databaseService() {
        return new DatabaseService();
    }
    
    @Bean
    public UserService userService() {
        return new UserService();
    }
    
    @Bean
    public VerificationService verificationService(UserService userService) {
        return new VerificationService(userService);
    }
    
    @Bean
    public VotingSystem votingSystem(DatabaseService databaseService) {
        return new VotingSystem(databaseService);
    }
}
