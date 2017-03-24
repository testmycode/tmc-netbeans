package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.ui.TmcNotificationDisplayer;

import java.awt.HeadlessException;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

import org.openide.awt.NotificationDisplayer;
import org.openide.util.ImageUtilities;

public class CheckForOutdatedNetbeans {
    
    private static final String OUTDATED_TITLE = "Please update your Netbeans.";
    private static final String OUTDATED_MESSAGE = "Your Netbeans is too old and it may not work with the TestMyCode plugin.\nWe recommend that you update your Netbeans to at least version 8.2.";
    private static final Logger logger = Logger.getLogger(CheckForOutdatedNetbeans.class.getName());
    
    public static void run() {
        TmcNotificationDisplayer.SingletonToken NOTIFIER_TOKEN = TmcNotificationDisplayer.createSingletonToken();
        TmcNotificationDisplayer notifier = TmcNotificationDisplayer.getDefault();
        try {
            final String hostProgramVersion = TmcSettingsHolder.get().hostProgramVersion().substring(0, 4);
            final int version = Integer.parseInt(hostProgramVersion);
            if (version > 2000 && version < 2015) {
                notifier.notify(NOTIFIER_TOKEN, OUTDATED_TITLE, ImageUtilities.loadImageIcon("fi/helsinki/cs/tmc/smile.gif", false), OUTDATED_MESSAGE, null, NotificationDisplayer.Priority.HIGH);
            }
        } catch (HeadlessException | NumberFormatException ex) {
            logger.warning("Could not check for an outdated Netbeans.");
        }
    }
}
