package fi.helsinki.cs.tmc.coreimpl;

import fi.helsinki.cs.tmc.core.configuration.TmcSettings;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.events.TmcEvent;
import fi.helsinki.cs.tmc.core.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
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
import org.openide.filesystems.FileUtil;
import org.openide.modules.Modules;
import org.openide.util.Lookup;

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
    private static final String PREF_RESOLVE_DEPENDENCIES = "resolveDependencies";
    private static final String PREF_SEND_DIAGNOSTICS = "sendDiagnostics";
    
    private static PersistableSettings settings = PersistableSettings.forModule(TmcSettings.class);
    
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
    public Optional<Course> getCurrentCourse() {
        return Optional.of(CourseDb.getInstance().getCurrentCourse());
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
    public String clientVersion() {
        return Modules.getDefault().ownerOf(TmcCoreSettingsImpl.class).getSpecificationVersion().toString();

    }

    @Override
    public String getFormattedUserData() {
        return "";
    }

    @Override
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
        CourseDb.getInstance().setCurrentCourseName(course.getName());
    }

    @Override
    public void setConfigRoot(Path path) {
        // NOP - can't change.
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

    @Override
    public String hostProgramName() {
        return "netbeans";
    }

    @Override
    public String hostProgramVersion() {
        return System.getProperty("netbeans.buildnumber");
    }
    
    public static class SavedEvent implements TmcEvent {}
    
    public TmcCoreSettingsImpl() {
        // NOP
    }
    
    /*package*/ TmcCoreSettingsImpl(PersistableSettings settings, Tailoring tailoring, TmcEventBus eventBus) {
        this.settings = settings;
        this.tailoring = tailoring;
        this.eventBus = eventBus;
        
        this.unsavedPassword = settings.get(PREF_PASSWORD, "");
    }
    
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
    
    public void setResolveDependencies(boolean value) {
        settings.put(PREF_RESOLVE_DEPENDENCIES, value ? "1" : "0");
    }
    
    public boolean getResolveDependencies() {
        return settings.get(PREF_RESOLVE_DEPENDENCIES, "1").equals("1");
    }

    public void setSendDiagnostics(boolean value) {
        settings.put(PREF_SEND_DIAGNOSTICS, value ? "1" : "0");
    }
    
    public boolean getSendDiagnostics() {
        return settings.get(PREF_SEND_DIAGNOSTICS, "1").equals("1");
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
