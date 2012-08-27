package fi.helsinki.cs.tmc.tailoring;

/**
 * Tailoring handy for those developing and debugging this plugin.
 */
public class DeveloperTailoring extends DefaultTailoring {
    @Override
    public String getDefaultServerUrl() {
        return "http://localhost:3000";
    }

    @Override
    public String getDefaultUsername() {
        return getSystemUsername();
    }
}
