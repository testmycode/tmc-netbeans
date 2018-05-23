package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.ui.AboutDialog;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "TMC",
        id = "fi.helsinki.cs.tmc.actions.ShowAboutAction")
@ActionRegistration(displayName = "#CTL_ShowAboutAction")
@ActionReferences({
    @ActionReference(path = "Menu/TM&C", position = 60)
})
@Messages("CTL_ShowAboutAction=About")
public class ShowAboutAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        AboutDialog.display();
    }
    
}
