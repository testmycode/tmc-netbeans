package fi.helsinki.cs.tmc.coreimpl;

import fi.helsinki.cs.tmc.core.configuration.TmcSettings;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.PersistableSettings;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.tailoring.SelectedTailoring;
import fi.helsinki.cs.tmc.tailoring.Tailoring;

import com.google.common.base.Optional;
import java.io.IOException;
import java.net.ProxySelector;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

public class TmcCoreSettingsImpl implements TmcSettings {

   
    private static final String PREF_BASE_URL = "baseUrl";
    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password";
    private static final String PREF_PROJECT_ROOT_DIR = "projectRootDir";
    private static final String PREF_CHECK_FOR_UPDATES_IN_BACKGROUND = "checkForUpdatesInBackground";
    private static final String PREF_CHECK_FOR_UNOPENED_AT_STARTUP = "checkForUnopenedAtStartup";
    private static final String PREF_SPYWARE_ENABLED = "spywareEnabled";
    private static final String PREF_DETAILED_SPYWARE_ENABLED = "detailedSpywareEnabled";
    private static final String PREF_ERROR_MSG_LOCALE = "errorMsgLocale";
    
    private static final PersistableSettings settings = PersistableSettings.forModule(TmcSettings.class);
    
    private Tailoring tailoring = SelectedTailoring.get();
    private TmcEventBus eventBus = TmcEventBus.getDefault();
    
    private String unsavedPassword = settings.get(PREF_PASSWORD, "");

    @Override
    public String getServerAddress() {
        return settings.get(PREF_BASE_URL, tailoring.getDefaultServerUrl());
    }

    @Override
    public boolean userDataExists() {
        return true;
    }

    @Override
    // TODO: replace this with courseDbs
    public Optional<Course> getCurrentCourse() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String apiVersion() {
        return "7";
    }

    @Override
    public String clientName() {
        return "netbeans_plugin";
    }

    @Override
    // TODO
    public String clientVersion() {
        return "0.9.2";
    }

    @Override
    // TODO what is this even
    public String getFormattedUserData() {
        return "";
    }

    @Override
    // TODO
    public Path getTmcProjectDirectory() {
        return Paths.get(getProjectRootDir());
    }

    @Override
    public Locale getLocale() {
        return getErrorMsgLocale();
    }

    @Override
    public SystemDefaultRoutePlanner proxy() {
        ProxySelector proxys = Lookup.getDefault().lookup((ProxySelector.class));
        return new SystemDefaultRoutePlanner(proxys);
    }

    @Override
    public void setCourse(Course course) {
        // TODO use this instead of coursedb
    }

    @Override
    public void setConfigRoot(Path path) {
        // NOP
    }

    @Override
    public Path getConfigRoot() {
        FileObject root = FileUtil.getConfigRoot();
        FileObject tmcRoot = root.getFileObject("tmc");

        if (tmcRoot == null) {
            try {
                tmcRoot = root.createFolder("tmc");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
       
        return FileUtil.toFile(tmcRoot).toPath();
    }
    
    public static class SavedEvent implements TmcEvent {}
    
//                PersistableSettings.forModule(TmcSettings.class),
//                SelectedTailoring.get(),
//                TmcEventBus.getDefault()
    
//    /*package*/ TmcSettings(PersistableSettings settings, Tailoring tailoring, TmcEventBus eventBus) {
//        this.settings = settings;
//        this.tailoring = tailoring;
//        this.eventBus = eventBus;
//        
//        this.unsavedPassword = settings.get(PREF_PASSWORD, "");
//    }
    
    public void save() {
        settings.saveAll();
        eventBus.post(new SavedEvent());        
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
    
    @Override
    public String getUsername() {
        return settings.get(PREF_USERNAME, tailoring.getDefaultUsername());
    }

    public void setUsername(String username) {
        settings.put(PREF_USERNAME, username);
    }
    
    @Override
    // TODO: make char array not to leave it in memory (String pool)
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
    
    public boolean isDetailedSpywareEnabled() {
        String defaultValue = tailoring.isDetailedSpywareEnabledByDefault() ? "1" : "0";
        return settings.get(PREF_DETAILED_SPYWARE_ENABLED, defaultValue).equals("1");
    }
    
    public Locale getErrorMsgLocale() {
        Locale dflt = tailoring.getDefaultErrorMsgLocale();
        return parseLocale(settings.get(PREF_ERROR_MSG_LOCALE, ""), dflt);
    }
    
    public void setErrorMsgLocale(Locale locale) {
        settings.put(PREF_ERROR_MSG_LOCALE, locale.toString());
    }
    
    private Locale parseLocale(String s, Locale dflt) {
        if (s.isEmpty()) {
            return dflt;
        }
        String[] parts = s.split("_");
        switch (parts.length) {
            case 1:
                return new Locale(parts[0]);
            case 2:
                return new Locale(parts[0], parts[1]);
            case 3:
                return new Locale(parts[0], parts[1], parts[2]);
            default:
                return dflt;
        }
    }
    
}
