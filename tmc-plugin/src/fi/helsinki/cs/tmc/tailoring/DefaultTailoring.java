package fi.helsinki.cs.tmc.tailoring;

import java.util.Locale;

public class DefaultTailoring implements Tailoring {
    @Override
    public String getDefaultServerUrl() {
        return "";
    }
    
    @Override
    public String getDefaultUsername() {
        return "";
    }
    
    @Override
    public String getUsernameFieldName() {
        return "username";
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

    @Override
    public boolean isSpywareEnabledByDefault() {
        return false;
    }
    
    @Override
    public boolean isDetailedSpywareEnabledByDefault() {
        return false;
    }
    
    @Override
    public Locale[] getAvailableErrorMsgLocales() {
        return new Locale[] { new Locale("en") };
    }
    
    @Override
    public Locale getDefaultErrorMsgLocale() {
        Locale def = Locale.getDefault();
        for (Locale avail : getAvailableErrorMsgLocales()) {
            if (def.getLanguage().equals(avail.getLanguage())) {
                return avail;
            }
        }
        return getAvailableErrorMsgLocales()[0];
    }

    @Override
    public String getUpdateCenterTitle() {
        return "TMC Updates";
    }

    @Override
    public String getUpdateCenterUrl() {
        return "http://update.testmycode.net/tmc-netbeans/updates.xml";
    }
}
