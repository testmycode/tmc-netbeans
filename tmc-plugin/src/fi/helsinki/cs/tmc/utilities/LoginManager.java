package fi.helsinki.cs.tmc.utilities;

import com.google.common.base.Optional;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.OauthCredentials;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.exceptions.AuthenticationFailedException;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.CourseListWindow;
import fi.helsinki.cs.tmc.ui.LoginDialog;
import fi.helsinki.cs.tmc.ui.OrganizationListWindow;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

public class LoginManager {
    
    private boolean ready;
    private Logger log = Logger.getLogger(LoginManager.class.getName());
    private AuthenticationFailedException authenticationException;

    public synchronized void login() throws InterruptedException, AuthenticationFailedException {
        if (LoginDialog.isWindowVisible() || OrganizationListWindow.isWindowVisible() || CourseListWindow.isWindowVisible()) {
            return;
        }
        ready = false;
        authenticationException = null;
        final Runnable closeHandler = new Runnable() {
            @Override
            public void run() {
                setReady(true);
            }
        };
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
                            if (ex instanceof AuthenticationFailedException) {
                                authenticationException = (AuthenticationFailedException) ex;
                                ConvenientDialogDisplayer.getDefault().displayError("Username or password is incorrect.", ex);
                            }
                        }
                        setReady(true);
                    }
                }, closeHandler);
            }
        });
        while (!ready) {
            Thread.sleep(1000);
        }
        
        if (authenticationException != null) {
            throw authenticationException;
        }
        showOrganizations();
    }
    
    public void setReady(boolean value) {
        this.ready = value;
    }
    
    private void showOrganizations() {
        if (TmcSettingsHolder.get().getOrganization() == null && loggedIn()) {
            try {
                OrganizationListWindow.display();
            } catch (Exception ex) {
                log.log(Level.WARNING, "Unable to show organizations", ex);
            }
        }
    }
    
    public void logout() {
        log.log(Level.INFO, "Logging out the user.");
        final TmcCoreSettingsImpl settings = (TmcCoreSettingsImpl) TmcSettingsHolder.get();
        settings.setToken(Optional.<String>absent());
        settings.setOauthCredentials(new OauthCredentials(null, null));
        settings.setUsername("");
        settings.setPassword(Optional.<String>absent());
        settings.setOrganization(null);
        CourseDb.getInstance().setCurrentCourseName(null);
        settings.save();
    }
    
    public static boolean loggedIn() {
        return TmcSettingsHolder.get().getToken().isPresent();
    }
}
