package fi.helsinki.cs.tmc.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.actions.Presenter;

@ActionID(category = "Palikka", id = "fi.helsinki.cs.tmc.ui.toolbars.TestAndSendToolbar")
@ActionRegistration(displayName = "(irrelevant)")
@ActionReferences({
    @ActionReference(path = "Toolbars/TestAndSendToolbar", position = 100)
})
/**
 * This class is used by NetBeans to create a toolbar for Palikka.
 */
public final class TestAndSendToolbar extends AbstractAction implements Presenter.Toolbar {

    /**
     * This is a dummy method that must be overridden. It doesn't do anything.
     * @param e 
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO dont' implement action body
    }

    /**
     * This method is invoked by NetBeans and should not be used.
     * Creates a new TestAndSendPanel and registers it at the controller. Now the controller knows of this toolbars existence and can add listeners to it.
     * @return Returns the created TestAndSendPanel to NetBeans
     */
    @Override
    public Component getToolbarPresenter() {
        return new TestAndSendPanel();
    }
}
