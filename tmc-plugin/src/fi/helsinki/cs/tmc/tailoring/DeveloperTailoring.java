package fi.helsinki.cs.tmc.tailoring;

import java.util.Locale;

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

    @Override
    public Locale[] getAvailableErrorMsgLocales() {
        return new Locale[] { new Locale("en"), new Locale("fi") };
    }

    @Override
    public Locale getDefaultErrorMsgLocale() {
        return new Locale("fi");
    }
}