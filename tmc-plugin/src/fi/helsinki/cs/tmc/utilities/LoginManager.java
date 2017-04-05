package fi.helsinki.cs.tmc.utilities;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.ui.LoginDialog;
import fi.helsinki.cs.tmc.ui.PreferencesUIFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

public class LoginManager {
    
    private boolean ready;
    private Logger log = Logger.getLogger(LoginManager.class.getName());
    
    public synchronized void login() throws InterruptedException {
        ready = false;
        // TODO: Remove this when we get rid of the login options in settings window
        if (PreferencesUIFactory.getInstance().isPreferencesUiVisible()) {
            return;
        }
        log.log(Level.INFO, "Asking the user to log in.");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LoginDialog.display(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            TmcCore.get().authenticate(ProgressObserver.NULL_OBSERVER, TmcSettingsHolder.get().getPassword().get()).call();
                        } catch (Exception ex) {
                            log.log(Level.WARNING, "Authentication failed!", ex);
                        }
                        setReady(true);
                    }
                });
            }
        });
        while (!ready) {
            Thread.sleep(1000);
        }
    }
    
    public void setReady(boolean value) {
        this.ready = value;
    }
}
