package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.ui.PreferencesUIFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import org.openide.DialogDescriptor;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "TMC",
id = "fi.helsinki.cs.tmc.actions.ShowSettings")
@ActionRegistration(displayName = "#CTL_ShowSettings")
@ActionReferences({
    @ActionReference(path = "Menu/TM&C", position = -100, separatorAfter = -90)
})
@Messages("CTL_ShowSettings=&Settings")
public final class ShowSettingsAction extends AbstractAction {

    private PreferencesUIFactory prefUiFactory;
    private SaveSettingsAction saveAction;
    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;

    public ShowSettingsAction() {
        this(PreferencesUIFactory.getInstance(),
                new SaveSettingsAction(),
                ServerAccess.getDefault(),
                CourseDb.getInstance(),
                ProjectMediator.getInstance());
    }

    public ShowSettingsAction(
            PreferencesUIFactory prefUiFactory,
            SaveSettingsAction saveAction,
            ServerAccess serverAccess,
            CourseDb courseDb,
            ProjectMediator projectMediator) {
        this.prefUiFactory = prefUiFactory;
        this.saveAction = saveAction;
        this.serverAccess = serverAccess;
        this.courseDb = courseDb;
        this.projectMediator = projectMediator;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (prefUiFactory.isPreferencesUiVisible()) {
            prefUiFactory.activateVisiblePreferencesUi();
            return;
        }
        
        final PreferencesUI prefUI = prefUiFactory.createCurrentPreferencesUI();

        prefUI.setUsername(serverAccess.getUsername());
        prefUI.setServerBaseUrl(serverAccess.getBaseUrl());
        prefUI.setProjectDir(projectMediator.getProjectRootDir());
        prefUI.setAvailableCourses(courseDb.getAvailableCourses());
        prefUI.setSelectedCourse(courseDb.getCurrentCourse());

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (event.getSource() == DialogDescriptor.OK_OPTION) {
                    ActionEvent okEvent = new ActionEvent(prefUI, ActionEvent.ACTION_PERFORMED, null);
                    saveAction.actionPerformed(okEvent);
                }
            }
        };
        
        prefUiFactory.showPreferencesDialog(listener);
    }
}
