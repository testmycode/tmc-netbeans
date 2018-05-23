package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.AbstractAction;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.HtmlBrowser;
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
            HtmlBrowser.URLDisplayer.getDefault().showURLExternal(new URL("https://tmc.mooc.fi/participants/me"));
        } catch (MalformedURLException ex) {
            ConvenientDialogDisplayer.getDefault().displayError("Failed to open browser.\n" + ex.getMessage());
        }
    }
    
}
