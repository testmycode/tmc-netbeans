package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.tailoring.SelectedTailoring;
import java.util.prefs.Preferences;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
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
                    showWelcomeDialog();
                    prefs.putBoolean(PREF_FIRST_RUN, false);
                }
            }
        });
    }
    
    private void showWelcomeDialog() {
        String msg = SelectedTailoring.get().getFirstRunMessage();
        String title = "TMC installed";
        
        DialogDescriptor dd = new DialogDescriptor(msg, title);
        dd.setModal(true);
        dd.setMessageType(DialogDescriptor.INFORMATION_MESSAGE);
        dd.setOptions(new Object[] { DialogDescriptor.OK_OPTION });
        dd.setButtonListener(new ShowSettingsAction());
        DialogDisplayer.getDefault().notifyLater(dd);
    }
}
