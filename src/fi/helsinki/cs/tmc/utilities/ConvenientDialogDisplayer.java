/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.utilities;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import fi.helsinki.cs.tmc.ui.LongTextDisplayPanel;
import java.awt.Component;

public class ConvenientDialogDisplayer {
    private static ConvenientDialogDisplayer defaultDisplayer;

    public ConvenientDialogDisplayer() {
    }
    
    public static ConvenientDialogDisplayer getDefault() {
        if (defaultDisplayer == null) {
            defaultDisplayer = new ConvenientDialogDisplayer();
        }

        return defaultDisplayer;
    }

    
    /**
     * Display exception error message.
     */
    public void displayError(Throwable e) {
        displayError(e.getMessage());
    }
    
    
    public void displayError(String errorMsg) {
        displayMessage(errorMsg, NotifyDescriptor.ERROR_MESSAGE);
    }
    
    public void displayWarning(String msg) {
        displayMessage(msg, NotifyDescriptor.WARNING_MESSAGE);
    }
    
    public void displayMessage(String msg) {
        displayMessage(msg, NotifyDescriptor.PLAIN_MESSAGE);
    }

    protected void displayMessage(String msg, int notifyType) {
        JPanel dialog = new JPanel();
        msg = "<html><pre>" + msg + "</pre></html>"; //multiline support        
        dialog.add(new JLabel(msg));
        
        showDialog(dialog, notifyType);
    }
    
    
    public void displayLongError(String errorMsg) {
        displayLongMessage(errorMsg, NotifyDescriptor.ERROR_MESSAGE);
    }
    
    public void displayLongMessage(String text) {
        displayLongMessage(text, NotifyDescriptor.PLAIN_MESSAGE);
    }
    
    private void displayLongMessage(String text, int notifyType) {
        LongTextDisplayPanel panel = new LongTextDisplayPanel(text);
        showDialog(panel, notifyType);
    }

    private void showDialog(Component dialog, int notifyType) {
        showDialog(dialog, notifyType, "");
    }
    
    private void showDialog(Component dialog, int notifyType, String title) {
        NotifyDescriptor descriptor = new NotifyDescriptor(
                dialog,
                title,
                NotifyDescriptor.OK_CANCEL_OPTION,
                notifyType,
                new Object[]{NotifyDescriptor.OK_OPTION},
                NotifyDescriptor.OK_OPTION
                );
        DialogDisplayer.getDefault().notify(descriptor);
    }
    
    
    /**
     * For displaying notification when all exercise tests passed.
     * @param message Message to be displayed.
     * @param title Dialog box title.
     */
    public void displayHappyMessage(String message, String title) {
        JPanel dialog = new JPanel();
        JLabel label = new JLabel(message);
        label.setIcon(new ImageIcon(getClass().getResource("/fi/helsinki/cs/tmc/smile.gif")));
        dialog.add(label);

        showDialog(dialog, NotifyDescriptor.PLAIN_MESSAGE, title);
    }
}
