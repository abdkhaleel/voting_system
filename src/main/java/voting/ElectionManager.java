package voting;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ElectionManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String STORAGE_FILE = "elections.dat";
    
    private List<Election> elections;
    
    public ElectionManager() {
        this.elections = new ArrayList<>();
    }
    
    public Election createElection(String title, String description, LocalDateTime startDate, LocalDateTime endDate) {
        Election election = new Election(title, description, startDate, endDate);
        elections.add(election);
        saveElections();
        return election;
    }
    
    public Optional<Election> getElection(String electionId) {
        return elections.stream()
                .filter(e -> e.getId().equals(electionId))
                .findFirst();
    }
    
    public List<Election> getAllElections() {
        return new ArrayList<>(elections);
    }
    
    public List<Election> getActiveElections() {
        return elections.stream()
                .filter(Election::isActive)
                .toList();
    }
    
    public void updateElection(Election election) {
        for (int i = 0; i < elections.size(); i++) {
            if (elections.get(i).getId().equals(election.getId())) {
                elections.set(i, election);
                saveElections();
                return;
            }
        }
    }
    
    public void deleteElection(String electionId) {
        elections.removeIf(e -> e.getId().equals(electionId));
        saveElections();
    }
    
    public void saveElections() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STORAGE_FILE))) {
            oos.writeObject(elections);
        } catch (IOException e) {
            System.err.println("Error saving elections: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    public void loadElections() {
        File file = new File(STORAGE_FILE);
        if (!file.exists()) {
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            elections = (List<Election>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading elections: " + e.getMessage());
        }
    }
}
