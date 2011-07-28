package fi.helsinki.cs.tmc.ui;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import java.awt.Component;
import org.openide.DialogDescriptor;

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
        showDialog(panel, notifyType, "", false);
    }

    private void showDialog(Component content, int notifyType) {
        showDialog(content, notifyType, "", true);
    }
    
    private void showDialog(Component content, int notifyType, String title, boolean modal) {
        DialogDescriptor desc = new DialogDescriptor(content,title);
        desc.setModal(modal);
        desc.setOptions(new Object[] { NotifyDescriptor.OK_OPTION });
        desc.setValue(NotifyDescriptor.OK_OPTION);
        desc.setMessageType(notifyType);
        DialogDisplayer.getDefault().notifyLater(desc);
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

        showDialog(dialog, NotifyDescriptor.PLAIN_MESSAGE, title, true);
    }
    
    
    public boolean askYesNo(String question, String title) {
        DialogDescriptor desc = new DialogDescriptor(question, title);
        desc.setModal(true);
        desc.setOptions(new Object[] { DialogDescriptor.YES_OPTION, DialogDescriptor.NO_OPTION });
        desc.setValue(DialogDescriptor.YES_OPTION);
        desc.setMessageType(DialogDescriptor.QUESTION_MESSAGE);
        Object response = DialogDisplayer.getDefault().notify(desc);
        return (response == DialogDescriptor.YES_OPTION);
    }
}
