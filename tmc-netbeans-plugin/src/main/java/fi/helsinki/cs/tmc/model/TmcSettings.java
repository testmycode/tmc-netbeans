package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.tailoring.SelectedTailoring;
import fi.helsinki.cs.tmc.tailoring.Tailoring;

/**
 * A transient saveable collection of all settings of the TMC plugin.
 */
public class TmcSettings {
    private static final String PREF_BASE_URL = "baseUrl";
    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password";
    private static final String PREF_PROJECT_ROOT_DIR = "projectRootDir";
    private static final String PREF_CHECK_FOR_UPDATES_IN_BACKGROUND = "checkForUpdatesInBackground";
    private static final String PREF_CHECK_FOR_UNOPENED_AT_STARTUP = "checkForUnopenedAtStartup";
    private static final String PREF_SPYWARE_ENABLED = "spywareEnabled";
    
    private static final TmcSettings defaultInstance =
            new TmcSettings(
                    PersistableSettings.forModule(TmcSettings.class),
                    SelectedTailoring.get()
                    );
    
    private PersistableSettings settings;
    private Tailoring tailoring;
    
    private String unsavedPassword;

    public static TmcSettings getDefault() {
        return defaultInstance;
    }
    
    public static TmcSettings getTransient() {
        return new TmcSettings(PersistableSettings.forModule(TmcSettings.class), SelectedTailoring.get());
    }
    
    /*package*/ TmcSettings(PersistableSettings settings, Tailoring tailoring) {
        this.settings = settings;
        this.tailoring = tailoring;
        
        this.unsavedPassword = settings.get(PREF_PASSWORD, "");
    }
    
    public void save() {
        if (this != defaultInstance) {
            throw new IllegalStateException("May only safe the default instance of TmcSettings.");
        }
        settings.saveAll();
    }

    public String getServerBaseUrl() {
        return settings.get(PREF_BASE_URL, tailoring.getDefaultServerUrl());
    }
    
    public void setServerBaseUrl(String baseUrl) {
        baseUrl = stripTrailingSlashes(baseUrl);
        settings.put(PREF_BASE_URL, baseUrl);
    }
    
    private String stripTrailingSlashes(String s) {
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
    
    public String getUsername() {
        return settings.get(PREF_USERNAME, tailoring.getDefaultUsername());
    }

    public void setUsername(String username) {
        settings.put(PREF_USERNAME, username);
    }
    
    public String getPassword() {
        return unsavedPassword;
    }
    
    public void setPassword(String password) {
        unsavedPassword = password;
        if (isSavingPassword()) {
            settings.put(PREF_PASSWORD, password);
        }
    }
    
    public void setSavingPassword(boolean shouldSave) {
        if (shouldSave) {
            settings.put(PREF_PASSWORD, unsavedPassword);
        } else {
            settings.remove(PREF_PASSWORD);
        }
    }
    
    public boolean isSavingPassword() {
        return settings.get(PREF_PASSWORD, null) != null;
    }
    
    public String getProjectRootDir() {
        String path = settings.get(PREF_PROJECT_ROOT_DIR, null);
        if (path != null) {
            return path;
        } else {
            // Can sometimes take a while. That's why we don't pass it as a default above.
            return ProjectMediator.getDefaultProjectRootDir();
        }
    }
    
    public void setProjectRootDir(String value) {
        settings.put(PREF_PROJECT_ROOT_DIR, value);
    }
    
    public boolean isCheckingForUpdatesInTheBackground() {
        return settings.get(PREF_CHECK_FOR_UPDATES_IN_BACKGROUND, "1").equals("1");
    }
    
    public void setCheckingForUpdatesInTheBackground(boolean value) {
        settings.put(PREF_CHECK_FOR_UPDATES_IN_BACKGROUND, value ? "1" : "0");
    }
    
    public boolean isCheckingForUnopenedAtStartup() {
        return settings.get(PREF_CHECK_FOR_UNOPENED_AT_STARTUP, "1").equals("1");
    }
    
    public void setCheckingForUnopenedAtStartup(boolean value) {
        settings.put(PREF_CHECK_FOR_UNOPENED_AT_STARTUP, value ? "1" : "0");
    }

    public boolean isSpywareEnabled() {
        String defaultValue = tailoring.isSpywareEnabledByDefault() ? "1" : "0";
        return settings.get(PREF_SPYWARE_ENABLED, defaultValue).equals("1");
    }
    
    public void setIsSpywareEnabled(boolean value) {
        settings.put(PREF_SPYWARE_ENABLED, value ? "1" : "0");
    }
}
