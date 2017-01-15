package fi.helsinki.cs.tmc.tailoring;

import java.util.Locale;

/**
 * Provides for minor modifications in default settings, text labels etc.
 * to suit customized installations.
 * 
 * @see <code>SelectedTailoring.properties.sample</code>
 */
public interface Tailoring {
    String getDefaultServerUrl();
    String getDefaultUsername();
    String getUsernameFieldName();
    boolean isSpywareEnabledByDefault();
    boolean isDetailedSpywareEnabledByDefault();
    
    Locale[] getAvailableErrorMsgLocales();
    Locale getDefaultErrorMsgLocale();
    
    String getUpdateCenterTitle();
    String getUpdateCenterUrl();
}
