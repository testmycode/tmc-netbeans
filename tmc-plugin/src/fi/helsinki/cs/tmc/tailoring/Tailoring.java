package fi.helsinki.cs.tmc.tailoring;

import java.util.Locale;

/**
 * Provides for minor modifications in default settings, text labels etc.
 * to suit customized installations.
 * 
 * @see <code>SelectedTailoring.properties.sample</code>
 */
public interface Tailoring {
    public String getDefaultServerUrl();
    public String getDefaultUsername();
    public String getUsernameFieldName();
    public boolean isSpywareEnabledByDefault();
    
    public Locale[] getAvailableErrorMsgLocales();
    public Locale getDefaultErrorMsgLocale();
    
    public String getUpdateCenterTitle();
    public String getUpdateCenterUrl();
}
