package fi.helsinki.cs.tmc.tailoring;

/**
 * Provides for minor modifications in default settings, text labels etc.
 * to suit customized installations.
 * 
 * @see <code>SelectedTailoring.properties.sample</code>
 */
public interface Tailoring {
    public String getDefaultServerUrl();
    public String getDefaultUsername();
    
    /**
     * A message dialog to be shown when the plugin is first installed
     * and before the settings screen opens automatically.
     */
    public String getFirstRunMessage();
}
