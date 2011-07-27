package fi.helsinki.cs.tmc.tailoring;

public class DeveloperTailoring extends DefaultTailoring {
    @Override
    public String getDefaultServerUrl() {
        return "http://localhost:3000";
    }
}
