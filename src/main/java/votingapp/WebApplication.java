package votingapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import user.DatabaseService;
import user.UserService;
import user.VerificationService;
import voting.VotingSystem;

import javax.annotation.PreDestroy;

@SpringBootApplication
@ComponentScan(basePackages = {"api", "blockchain", "user", "voting", "config", "exception", "util", "votingapp"})
public class WebApplication {
    
    private static ConfigurableApplicationContext context;
    
    public static void main(String[] args) {
        context = SpringApplication.run(WebApplication.class, args);
        
        // Add a shutdown hook to ensure clean shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (context != null && context.isActive()) {
                VotingSystem votingSystem = context.getBean(VotingSystem.class);
                votingSystem.saveBlockchainState();
                System.out.println("Blockchain state saved during shutdown");
            }
        }));
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
    
    @PreDestroy
    public void onShutdown() {
        // This method will be called when the Spring context is being destroyed
        VotingSystem votingSystem = context.getBean(VotingSystem.class);
        votingSystem.saveBlockchainState();
        System.out.println("Blockchain state saved during application shutdown");
    }
}
