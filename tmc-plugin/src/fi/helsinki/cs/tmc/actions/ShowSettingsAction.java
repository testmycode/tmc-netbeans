package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.NbTmcSettings;
import fi.helsinki.cs.tmc.tailoring.SelectedTailoring;
import fi.helsinki.cs.tmc.tailoring.Tailoring;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.ui.PreferencesUIFactory;

import org.openide.DialogDescriptor;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;

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
    private CourseDb courseDb;
    private Tailoring tailoring;

    public ShowSettingsAction() {
        this.prefUiFactory = PreferencesUIFactory.getInstance();
        this.saveAction = new SaveSettingsAction();
        this.courseDb = CourseDb.getInstance();
        this.tailoring = SelectedTailoring.get();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }

    public void run() {
        if (prefUiFactory.isPreferencesUiVisible()) {
            prefUiFactory.activateVisiblePreferencesUi();
            return;
        }

        final PreferencesUI prefUI = prefUiFactory.createCurrentPreferencesUI();

        NbTmcSettings settings = NbTmcSettings.getDefault();
        prefUI.setUsername(settings.getUsername());
        prefUI.setPassword(settings.getPassword());
        prefUI.setShouldSavePassword(settings.isSavingPassword());
        prefUI.setServerBaseUrl(settings.getServerAddress());
        prefUI.setProjectDir(settings.getTmcMainDirectory());
        prefUI.setCheckForUpdatesInTheBackground(settings.isCheckingForUpdatesInTheBackground());
        prefUI.setCheckForUnopenedExercisesAtStartup(settings.isCheckingForUnopenedAtStartup());
        prefUI.setAvailableCourses(courseDb.getAvailableCourses());
        prefUI.setSelectedCourseName(courseDb.getCurrentCourseName());
        prefUI.setUsernameFieldName(tailoring.getUsernameFieldName());
        prefUI.setSpywareEnabled(settings.isSpywareEnabled());
        prefUI.setErrorMsgLocale(settings.getErrorMsgLocale());

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