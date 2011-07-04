/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.utilities;

import java.awt.Dialog;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import fi.helsinki.cs.tmc.ui.swingPanels.TestResultPanel;

/**
 *
 * @author knordman
 */
public class ModalDialogDisplayer {

    private static ModalDialogDisplayer defaultDisplayer;

    public enum buttonResponse {

        ok,
        cancel
    }

    /**
     * Constructor
     */
    public ModalDialogDisplayer() {
    }

    
    
    /**
     * Get ModalDialogDisplayer.
     * @return ModalDialogDisplayer
     */
    public static ModalDialogDisplayer getDefault() {
        if (defaultDisplayer == null) {
            defaultDisplayer = new ModalDialogDisplayer();
        }

        return defaultDisplayer;
    }

    
    
    /**
     * Display exception error message.
     * @param e Exception
     */
    public void displayError(Exception e) {
        displayError(e.getMessage());
    }

    
    
    /**
     * Display error message.
     * @param errorMsg Error message.
     */
    public void displayError(String errorMsg) {
        JPanel errorDialog = new JPanel();

        errorMsg = "<html><pre>" + errorMsg + "</pre></html>"; //multiline support        
        errorDialog.add(new JLabel(errorMsg));

        NotifyDescriptor descriptor = new NotifyDescriptor(errorDialog,
                "Error",
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.ERROR_MESSAGE,
                new Object[]{NotifyDescriptor.OK_OPTION},
                NotifyDescriptor.OK_OPTION);
        DialogDisplayer.getDefault().notify(descriptor);

    }

    
    
    /**
     * Display notification message.
     * @param message Message to be displayed.
     * @param title Dialog box title.
     */
    public void displayNotification(String message, String title) {
        JPanel resultsDialog = new JPanel();
        JLabel label = new JLabel(message);

        resultsDialog.add(label);

        NotifyDescriptor descriptor = new NotifyDescriptor(resultsDialog,
                title,
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.WARNING_MESSAGE,
                new Object[]{NotifyDescriptor.OK_OPTION},
                NotifyDescriptor.OK_OPTION);

        Object notify = DialogDisplayer.getDefault().notify(descriptor);
    }
    
    
    
    /**
     * Display test results dialog.
     * @param messageArray Test results.
     */
    public void showTestResultDialog(ArrayList<String> messageArray) {

        TestResultPanel panel = new TestResultPanel(messageArray);

        DialogDescriptor descriptor = new DialogDescriptor(panel,
                "Testresults",
                true,
                NotifyDescriptor.OK_CANCEL_OPTION,
                new Object[]{NotifyDescriptor.OK_OPTION},
                null);

        Dialog dlg = DialogDisplayer.getDefault().createDialog(descriptor);
        dlg.setResizable(false);
        dlg.setVisible(true);
    }

    
    
    /**
     * Display notification message when all exercise tests passed.
     * @param message Message to be displayed.
     * @param title Dialog box title.
     */
    public void displayHappyNotification(String message, String title) {
        JPanel resultsDialog = new JPanel();
        JLabel label = new JLabel(message);

        label.setIcon(new ImageIcon(getClass().getResource("/palikka/smile.gif")));

        resultsDialog.add(label);

        NotifyDescriptor descriptor = new NotifyDescriptor(resultsDialog,
                title,
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.PLAIN_MESSAGE,
                new Object[]{NotifyDescriptor.OK_OPTION},
                NotifyDescriptor.OK_OPTION);

        Object notify = DialogDisplayer.getDefault().notify(descriptor);
    }
}
