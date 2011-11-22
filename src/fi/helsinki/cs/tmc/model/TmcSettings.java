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
    
    private PersistableSettings settings;
    private Tailoring tailoring;
    
    private String unsavedPassword;

    public static TmcSettings getSaved() {
        return new TmcSettings(
                PersistableSettings.forModule(TmcSettings.class),
                SelectedTailoring.get()
                );
    }
    
    public TmcSettings(PersistableSettings settings, Tailoring tailoring) {
        this.settings = settings;
        this.tailoring = tailoring;
        
        this.unsavedPassword = settings.get(PREF_PASSWORD, "");
    }
    
    public void save() {
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
        return settings.get(PREF_PROJECT_ROOT_DIR, ProjectMediator.getDefaultProjectRootDir());
    }
    
    public void setProjectRootDir(String value) {
        settings.put(PREF_PROJECT_ROOT_DIR, value);
    }
    
}
