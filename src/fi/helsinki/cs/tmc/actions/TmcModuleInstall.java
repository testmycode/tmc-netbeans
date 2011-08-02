package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.tailoring.SelectedTailoring;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.NotificationDisplayer;
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
                } else {
                    new CheckForNewExercises(new OpenExercisesAction()).actionPerformed(null);
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
        
        /*
         * We need to wrap the showing of the settings window in invokeLater.
         * Otherwise the settings window may get the welcome dialog as its
         * parent, which leads to funky window focus problems such as
         * the settings window dropping behind the NetBeans window after
         * an error dialog from it is closed.
         */
        dd.setButtonListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        new ShowSettingsAction().actionPerformed(null);
                    }
                });
            }
        });
        
        DialogDisplayer.getDefault().notifyLater(dd);
    }
}
