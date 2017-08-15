package fi.helsinki.cs.tmc.tasks;

import fi.helsinki.cs.tmc.core.exceptions.NotLoggedInException;
import fi.helsinki.cs.tmc.utilities.LoginManager;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class LoginTask implements Callable<Void> {
    private static final Logger log = Logger.getLogger(LoginTask.class.getName());

    @Override
    public Void call() throws Exception {
        log.fine("Showing the login window.");
        try {
            new LoginManager().login();
        } catch (Exception ex) {
            throw new NotLoggedInException();
        }
        return null;
    }
    
}
