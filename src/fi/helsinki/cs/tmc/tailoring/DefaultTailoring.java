package fi.helsinki.cs.tmc.tailoring;

public class DefaultTailoring implements Tailoring {
    @Override
    public String getDefaultServerUrl() {
        return "";
    }
    
    @Override
    public String getDefaultUsername() {
        return "";
    }
    
    
    protected String getSystemUsername() {
        String user = System.getenv("USER"); // Unix
        if (user == null) {
            user = System.getenv("USERNAME"); // Windows
        }
        if (user == null) {
            user = "";
        }
        return user;
    }
}
