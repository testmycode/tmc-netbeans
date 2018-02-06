package fi.helsinki.cs.tmc.utilities;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.actions.ShowSettingsAction;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.configuration.TmcSettings;
import fi.helsinki.cs.tmc.core.domain.OauthCredentials;
import fi.helsinki.cs.tmc.core.domain.Organization;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.events.TmcEventBus;
import fi.helsinki.cs.tmc.core.exceptions.AuthenticationFailedException;
import fi.helsinki.cs.tmc.core.exceptions.ShowToUserException;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.core.utilities.TmcServerAddressNormalizer;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;
import fi.helsinki.cs.tmc.events.LoginStateChangedEvent;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.tailoring.SelectedTailoring;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.CourseListWindow;
import fi.helsinki.cs.tmc.ui.LoginDialog;
import fi.helsinki.cs.tmc.ui.OrganizationListWindow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

public class LoginManager {
    
    private boolean ready;
    private Logger log = Logger.getLogger(LoginManager.class.getName());
    private AuthenticationFailedException authenticationException;
    private IOException connectionException;
    private final TmcEventBus bus;

    public LoginManager() {
        this.bus = TmcEventBus.getDefault();
    }

    public synchronized void login() throws InterruptedException, AuthenticationFailedException, IOException {
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
                if (loggedIn()) {
                    showOrganizationsOrCourses();
                } else {
                    LoginDialog.display(new LoginListener(password -> {
                        try {
                            TmcServerAddressNormalizer normalizer = new TmcServerAddressNormalizer();
                            normalizer.normalize();
                            TmcCore.get().authenticate(ProgressObserver.NULL_OBSERVER, password).call();
                            normalizer.selectOrganizationAndCourse();
                        } catch (Exception ex) {
                            log.log(Level.WARNING, "Error while logging in! " + ex.toString());
                            final ConvenientDialogDisplayer displayer = ConvenientDialogDisplayer.getDefault();
                            final TmcSettings settings = TmcSettingsHolder.get();
                            final String serverAddress = settings.getServerAddress();
                            if (ex instanceof IOException) {
                                connectionException = (IOException) ex;
                                displayer.displayError("Couldn't connect to the server. Please check your internet connection.");
                                if (ex instanceof UnknownHostException) {
                                    displayer.displayError("Couldn't connect to the server. Please check your internet connection.");
                                } else if (ex instanceof FileNotFoundException) {
                                    if (serverAddress.contains("tmc.mooc.fi/mooc")) {
                                        displayer.displayError("The server https://tmc.mooc.fi/mooc is no longer supported by this client.\n" +
                                                "All the courses have been migrated to our main server https://tmc.mooc.fi.\n" +
                                                "If you'd like to do the migrated courses, you'll need to update your server address to\n" +
                                                "https://tmc.mooc.fi and create a new account there.\n" +
                                                "After that, choose the MOOC organization after logging in.\n\n" +
                                                "For more information, check the course materials on mooc.fi.");
                                    } else {
                                        displayer.displayError("Server address is incorrect or the server is not supported.");
                                    }
                                } else if (ex instanceof MalformedURLException) {
                                    displayer.displayError("Malformed server address! Resetting to default.");
                                    final String defaultServerUrl = SelectedTailoring.get().getDefaultServerUrl();
                                    settings.setServerAddress(defaultServerUrl);
                                }
                            } else if (ex instanceof AuthenticationFailedException) {
                                authenticationException = (AuthenticationFailedException) ex;
                                displayer.displayError("Username or password is incorrect.", ex);
                            } else if (ex instanceof ShowToUserException) {
                                displayer.displayError(ex.getMessage());
                            } else {
                                if (serverAddress.contains("tmc.mooc.fi") && !serverAddress.contains("https://")) {
                                    displayer.displayError("Malformed server address! Resetting to default.");
                                    final String defaultServerUrl = SelectedTailoring.get().getDefaultServerUrl();
                                    settings.setServerAddress(defaultServerUrl);
                                } else {
                                    displayer.displayError("Logging in failed! Try again.");
                                }
                            }
                        }
                        setReady(true);
                        bus.post(new LoginStateChangedEvent());
                    }), closeHandler);
                }
            }
        });
        while (!ready) {
            Thread.sleep(1000);
        }
        
        if (authenticationException != null) {
            throw authenticationException;
        }
        if (connectionException != null) {
            throw connectionException;
        }
        if (loggedIn()) {
            showOrganizationsOrCourses();
        }
    }
    
    public void setReady(boolean value) {
        this.ready = value;
    }
    
    private void showOrganizationsOrCourses() {
        TmcSettings settings = TmcSettingsHolder.get();
        if (!settings.getOrganization().isPresent()) {
            try {
                OrganizationListWindow.display();
            } catch (Exception ex) {
                log.log(Level.WARNING, "Unable to show organizations", ex);
            }
        } else if (!settings.getCurrentCourse().isPresent()) {
            try {
                CourseListWindow.display();
            } catch (Exception ex) {
                log.log(Level.WARNING, "Unable to show courses", ex);
            }
        } else {
            new ShowSettingsAction().run();
        }
    }
    
    public void logout() {
        log.log(Level.INFO, "Logging out the user.");
        final TmcCoreSettingsImpl settings = (TmcCoreSettingsImpl) TmcSettingsHolder.get();
        settings.setToken(Optional.<String>absent());
        settings.setOauthCredentials(Optional.<OauthCredentials>absent());
        settings.setUsername("");
        settings.setPassword(Optional.<String>absent());
        settings.setOrganization(Optional.<Organization>absent());
        CourseDb.getInstance().setCurrentCourseName(null);
        settings.save();
        bus.post(new LoginStateChangedEvent());
    }
    
    public static boolean loggedIn() {
        return TmcSettingsHolder.get().getToken().isPresent();
    }
}
