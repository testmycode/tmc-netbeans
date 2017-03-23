package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.ui.TmcNotificationDisplayer;
import javax.swing.JOptionPane;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.ImageUtilities;

public class CheckForOutdatedNetbeans {
    
    private static final String OUTDATED_TITLE = "Please update your Netbeans.";
    private static final String OUTDATED_MESSAGE = "Your Netbeans is too old and doesn't work with the TestMyCode plugin.\nWe recommend that you update your Netbeans to at least version 8.2.";
    
    public static void run() {
        TmcNotificationDisplayer.SingletonToken NOTIFIER_TOKEN = TmcNotificationDisplayer.createSingletonToken();
        TmcNotificationDisplayer notifier = TmcNotificationDisplayer.getDefault();
        try {
            final String hostProgramVersion = TmcSettingsHolder.get().hostProgramVersion().substring(0, 4);
            final int version = Integer.parseInt(hostProgramVersion);
            if (version > 2000 && version < 2015) {
                JOptionPane.showMessageDialog(null, OUTDATED_MESSAGE, OUTDATED_TITLE, JOptionPane.ERROR_MESSAGE);
                notifier.notify(NOTIFIER_TOKEN, OUTDATED_TITLE, ImageUtilities.loadImageIcon("fi/helsinki/cs/tmc/smile.gif", false), OUTDATED_MESSAGE, null, NotificationDisplayer.Priority.HIGH);
            }
        } catch (Exception ex) {
        }
    }
}
