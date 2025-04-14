package voting;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AuditLog implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String LOG_FILE = "audit_log.txt";
    
    private final List<LogEntry> entries;
    
    public AuditLog() {
        this.entries = new ArrayList<>();
    }
    
    public void logEvent(String message) {
        LogEntry entry = new LogEntry(LocalDateTime.now(), message);
        entries.add(entry);
        writeToFile(entry);
    }
    
    private void writeToFile(LogEntry entry) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println(entry.toString());
        } catch (IOException e) {
            System.err.println("Error writing to audit log file: " + e.getMessage());
        }
    }
    
    public List<LogEntry> getEntries() {
        return new ArrayList<>(entries);
    }
    
    public static class LogEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        private final LocalDateTime timestamp;
        private final String message;
        
        public LogEntry(LocalDateTime timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return "[" + timestamp.format(FORMATTER) + "] " + message;
        }
    }
}
