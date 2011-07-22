package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.Refactored;
import fi.helsinki.cs.tmc.ui.PreferencesPanel;
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
@Refactored
public final class ShowSettingsAction extends AbstractAction {

    private DialogDisplayer displayer;
    private SaveSettingsAction saveAction;

    public ShowSettingsAction() {
        this(DialogDisplayer.getDefault(), new SaveSettingsAction());
    }

    public ShowSettingsAction(DialogDisplayer displayer, SaveSettingsAction saveAction) {
        this.displayer = displayer;
        this.saveAction = saveAction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final PreferencesPanel prefPanel = new PreferencesPanel();
        
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (event.getSource() == DialogDescriptor.OK_OPTION) {
                    ActionEvent okEvent = new ActionEvent(prefPanel, ActionEvent.ACTION_PERFORMED, null);
                    saveAction.actionPerformed(okEvent);
                }
            }
        };
        
        DialogDescriptor descriptor = new DialogDescriptor(
                prefPanel,
                "Preferences",
                true,
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.PLAIN_MESSAGE,
                listener);

        Dialog prefDialog = displayer.createDialog(descriptor);
        prefDialog.setResizable(false);
        prefDialog.setVisible(true);
    }
}
