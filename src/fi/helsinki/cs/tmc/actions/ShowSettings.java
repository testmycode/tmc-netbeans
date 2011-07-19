package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.settings.PluginSettings;
import fi.helsinki.cs.tmc.settings.Settings;
import fi.helsinki.cs.tmc.ui.swingPanels.PreferencesPanel;
import fi.helsinki.cs.tmc.utilities.ModalDialogDisplayer;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "TMC",
id = "fi.helsinki.cs.tmc.actions.ShowSettings")
@ActionRegistration(displayName = "#CTL_ShowSettings")
@ActionReferences({
    @ActionReference(path = "Menu/TMC", position = -100, separatorAfter = -90)
})
@Messages("CTL_ShowSettings=Show settings")
public final class ShowSettings extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        Settings settings = PluginSettings.getSettings();

        PreferencesPanel panel = new PreferencesPanel(settings);

        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    if (event.getSource() == DialogDescriptor.OK_OPTION) {
                        PluginSettings.saveSettings();
                    } else {
                        PluginSettings.loadFromFile(); //this erases all changes which user didn't want to save.
                    }
                } catch (Exception e) {
                    ModalDialogDisplayer.getDefault().displayError(e);
                }
            }
        };

        DialogDescriptor descriptor = new DialogDescriptor(
                panel,
                "Preferences",
                true,
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.PLAIN_MESSAGE,
                listener);

        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.setResizable(false);
        dialog.setVisible(true);
        panel.interruptCourseListUpdate();
    }
}
