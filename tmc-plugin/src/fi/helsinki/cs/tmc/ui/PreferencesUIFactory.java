package fi.helsinki.cs.tmc.ui;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.SwingUtilities;
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
    private Dialog dialog;

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

    public boolean isPreferencesUiVisible() {
        return dialog != null;
    }

    public void activateVisiblePreferencesUi() {
        if (dialog == null) {
            throw new IllegalStateException("Preferences UI not visible");
        }
        dialog.requestFocus();
    }

    /**
     * Shows the preferences dialog.
     *
     * <p>
     * This must be called after {@link #createCurrentPreferencesUI()} but not
     * twice without creating a new preferences UI in between.
     *
     * <p>
     * The <code>dialogListener</code> shall receive an event with either the OK
     * or cancel button constant in <code>DialogDescriptor</code> as the event
     * source. After the event is processed, the current panel is forgotten and
     * {@link #getCurrentPanel()} shall return null again.
     */
    public void showPreferencesDialog(final ActionListener dialogListener) {
        if (panel == null) {
            throw new IllegalStateException("Preferences UI not created yet");
        }
        if (dialog != null) {
            throw new IllegalStateException("Preferences UI already visible");
        }

        ActionListener closeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                destroyDialog();
                dialogListener.actionPerformed(e);
            }
        };

        DialogDescriptor descriptor = new DialogDescriptor(
                panel,
                "TMC Settings",
                false,
                NotifyDescriptor.OK_CANCEL_OPTION,
                null,
                closeListener);
        dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent we) {
                destroyDialog();
            }
        });
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dialog.setVisible(true);
            }
        });
    }

    private void destroyDialog() {
        panel = null;
        dialog = null;
    }
}
