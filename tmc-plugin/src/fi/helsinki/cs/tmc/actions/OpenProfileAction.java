package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BrowserOpener;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.AbstractAction;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(category = "TMC",
        id = "fi.helsinki.cs.tmc.actions.OpenProfileAction")
@ActionRegistration(displayName = "#CTL_OpenProfileAction")
@ActionReferences({
    @ActionReference(path = "Menu/TM&C", position = 50, separatorBefore = 49)
})
@NbBundle.Messages("CTL_OpenProfileAction=Open my profile")
public class OpenProfileAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            BrowserOpener.openUrl(URI.create("https://tmc.mooc.fi/participants/me"));
        } catch (URISyntaxException | IOException ex) {
            ConvenientDialogDisplayer.getDefault().displayError("Failed to open browser.\n" + ex.getMessage());
        }
    }

}
