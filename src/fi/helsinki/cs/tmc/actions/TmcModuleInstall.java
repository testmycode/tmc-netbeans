package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.ui.LoginDialog;
import java.util.prefs.Preferences;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

public class TmcModuleInstall extends ModuleInstall {
    private static final String PREF_FIRST_RUN = "firstRun";
    
    @Override
    public void restored() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            @Override
            public void run() {
                Preferences prefs = NbPreferences.forModule(TmcModuleInstall.class);
                
                boolean isFirstRun = prefs.getBoolean(PREF_FIRST_RUN, true);
                if (isFirstRun) {
                    doFirstRun();
                    prefs.putBoolean(PREF_FIRST_RUN, false);
                } else if (new ServerAccess().needsOnlyPassword()) {
                    LoginDialog.display(new CheckForNewExercisesOrUpdates(true));
                } else {
                    new CheckForNewExercisesOrUpdates(true).run();
                }
            }
        });
    }
    
    private void doFirstRun() {
        new ShowSettingsAction().run();
    }
}
