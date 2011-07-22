package fi.helsinki.cs.tmc.ui;

import java.awt.Dialog;
import java.awt.event.ActionListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 * Wraps PreferencesUI and shows it in a dialog on request.
 */
public class PreferencesUIFactory {

    private static PreferencesUIFactory instance;
    
    public static PreferencesUIFactory getInstance() {
        if (instance == null) {
            instance = new PreferencesUIFactory();
        }
        return instance;
    }
    
    private PreferencesPanel panel;
    
    /*package*/ PreferencesUIFactory() {
    }
    
    /**
     * Returns the currently visible preferences UI, if any.
     */
    public PreferencesUI getCurrentUI() {
        return panel;
    }
    
    /**
     * Creates a new current preferences UI but does not show it yet.
     */
    public PreferencesUI createCurrentPreferencesUI() {
        this.panel = new PreferencesPanel();
        return this.panel;
    }
    
    /**
     * Shows the preferences dialog.
     * 
     * <p>
     * This must be called after
     * {@link #createCurrentPreferencesUI()}.
     * 
     * <p>
     * The <code>dialogListener</code> shall receive an event with either
     * the OK or cancel button constant in <code>DialogDescriptor</code>
     * as the event source. After the event is processed, the current
     * panel is forgotten and {@link #getCurrentPanel()} shall return null
     * again.
     */
    public void showPreferencesDialog(ActionListener dialogListener) {
        DialogDescriptor descriptor = new DialogDescriptor(
                panel,
                "Settings",
                true,
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.PLAIN_MESSAGE,
                dialogListener);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.setVisible(true);
    }
}
