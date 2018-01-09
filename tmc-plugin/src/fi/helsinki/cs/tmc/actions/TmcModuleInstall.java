package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.configuration.TmcSettings;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.events.TmcEventBus;
import fi.helsinki.cs.tmc.core.holders.TmcLangsHolder;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;
import fi.helsinki.cs.tmc.events.LoginStateChangedEvent;
import fi.helsinki.cs.tmc.langs.util.TaskExecutorImpl;
import fi.helsinki.cs.tmc.spywareLocal.SpywareFacade;
import fi.helsinki.cs.tmc.tasks.LoginTask;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.LoginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.netbeans.api.autoupdate.UpdateUnitProvider;
import org.netbeans.api.autoupdate.UpdateUnitProviderFactory;
import org.openide.modules.ModuleInfo;
import org.openide.modules.ModuleInstall;
import org.openide.modules.Modules;
import org.openide.modules.SpecificationVersion;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

public class TmcModuleInstall extends ModuleInstall {

    private static final String PREF_FIRST_RUN = "firstRun";
    private static final String PREF_MODULE_VERSION = "moduleVersion";

    private static final Logger log = Logger.getLogger(TmcModuleInstall.class.getName());

    @Override
    public void restored() {
        TmcSettingsHolder.set(new TmcCoreSettingsImpl());
        TmcLangsHolder.set(new TaskExecutorImpl());

        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            @Override
            public void run() {
                MigrateSettings.run();

                CheckForOutdatedNetbeans.run();

                TmcCore.setInstance(new TmcCore());

                CheckForNewExercisesOrUpdates.startTimer();
                CheckForNewReviews.startTimer();
                ReviewEventListener.start();
                // Push events are disabled for now since this spams threads and
                // its functionality is rarely used since code reviews trigger
                // an email notification.
                // PushEventListener.start();
                TmcSettings settings = TmcSettingsHolder.get();
                SpywareFacade.start();

                Preferences prefs = NbPreferences.forModule(TmcModuleInstall.class);

                SpecificationVersion currentVersion = getCurrentModuleVersion();
                SpecificationVersion prevVersion = new SpecificationVersion(prefs.get(PREF_MODULE_VERSION, "0.0.0"));
                if (!currentVersion.equals(prevVersion)) {
                    try {
                        doUpdateFromPreviousVersion(prevVersion);
                    } catch (Exception ex) {
                        log.log(Level.WARNING, "Error while upgrading from previous version", ex);
                    }
                    prefs.put(PREF_MODULE_VERSION, currentVersion.toString());
                }
                
                new EnsureMavenBinaryIsExecutable().run();

                if (settings.getPassword().isPresent()) {
                    MigrateSettings.tryToMigratePasswordToOAuthToken();
                }

                boolean isFirstRun = prefs.getBoolean(PREF_FIRST_RUN, true);
                if (isFirstRun) {
                    doFirstRun();
                    prefs.putBoolean(PREF_FIRST_RUN, false);
                } else {
                    // Do full refresh.
                    if (!LoginManager.loggedIn()) {
                        BgTask.start("Asking user to log in", new LoginTask());
                    } else if (LoginManager.loggedIn() && settings.getOrganization().isPresent() && settings.getCurrentCourse().isPresent()) {
                        new RefreshCoursesAction().addDefaultListener(false, true).addListener(new BgTaskListener<List<Course>>() {
                            @Override
                            public void bgTaskReady(List<Course> result) {
                                log.warning("moduleInstall refresh ready");
                                new CheckForNewExercisesOrUpdates(true, false).run();
                                if (CheckForUnopenedExercises.shouldRunOnStartup()) {
                                    new CheckForUnopenedExercises().run();
                                }
                                new CheckProjectCount().checkAndNotifyIfOver();
                            }

                            @Override
                            public void bgTaskCancelled() {
                                log.warning("moduleInstall refresh cancelled");
                            }

                            @Override
                            public void bgTaskFailed(Throwable ex) {
                                log.log(Level.WARNING, "moduleInstall refresh failed ", ex);
                            }
                        }).run();
                    }
                }
                if (!isFirstRun && settings.getSendDiagnostics()) {
                    new SendDiagnostics().run();
                }
            }
        });
    }

    private SpecificationVersion getCurrentModuleVersion() {
        ModuleInfo modInfo = Modules.getDefault().ownerOf(this.getClass());
        return modInfo.getSpecificationVersion();
    }

    @Override
    public void close() {
        try {
            SpywareFacade.close();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to close SpywareFacade.", e);
        }
    }

    private void doFirstRun() {
        BgTask.start("First run", new LoginTask());
    }

    private void doUpdateFromPreviousVersion(SpecificationVersion prevVersion) {
        // A previous update center was registered with an incorrect name in versions before 0.3.1.
        // Moreover the URL has since changed. This version provides the new update center,
        // and here we ensure here that the old one is removed.
        if (prevVersion.compareTo(new SpecificationVersion("0.3.1")) <= 0) {
            removeOldUpdateCenterFromBefore031();
        }
    }

    private void removeOldUpdateCenterFromBefore031() {
        ArrayList<UpdateUnitProvider> providersToRemove = new ArrayList<UpdateUnitProvider>();
        for (UpdateUnitProvider provider : UpdateUnitProviderFactory.getDefault().getUpdateUnitProviders(false)) {
            if (provider == null || provider.getProviderURL() == null) {
                continue; // Users have had NPEs here :(
            }
            String url = provider.getProviderURL().toString();
            if (url.startsWith("http://tmc.mooc.fi/updates") && !url.contains("tmc-netbeans-author")) {
                log.log(Level.INFO, "Removing obsolete update center: {0}", provider.getDisplayName());
                providersToRemove.add(provider);
            }
        }

        for (UpdateUnitProvider provider : providersToRemove) {
            UpdateUnitProviderFactory.getDefault().remove(provider);
        }
    }
}
