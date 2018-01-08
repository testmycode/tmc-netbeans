package fi.helsinki.cs.tmc.utilities;

import fi.helsinki.cs.tmc.core.exceptions.AuthenticationFailedException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;

public class LoginListener implements ActionListener {
    private String password;
    private final Consumer<String> onPerformed;
    
    public LoginListener(Consumer<String> onPerformed) {
        this.onPerformed = onPerformed;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            onPerformed.accept(password);
        } catch (Exception ex) {
            if (ex instanceof AuthenticationFailedException || ex instanceof OAuthProblemException) {
                return;
            }
            throw ex;
        }
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
}
