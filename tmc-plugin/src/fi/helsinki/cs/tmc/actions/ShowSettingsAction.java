package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.tailoring.SelectedTailoring;
import fi.helsinki.cs.tmc.tailoring.Tailoring;
import fi.helsinki.cs.tmc.tasks.LoginTask;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.ui.PreferencesUIFactory;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.LoginManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.openide.DialogDescriptor;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.Exceptions;
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
    private CourseDb courseDb;
    private Tailoring tailoring;
    private Logger log = Logger.getLogger(ShowSettingsAction.class.getName());

    public ShowSettingsAction() {
        this.prefUiFactory = PreferencesUIFactory.getInstance();
        this.saveAction = new SaveSettingsAction();
        this.courseDb = CourseDb.getInstance();
        this.tailoring = SelectedTailoring.get();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!LoginManager.loggedIn()) {
            BgTask.start("Login window.", new LoginTask());
        } else {
            run();
        }
    }

    public void run() {
        if (prefUiFactory.isPreferencesUiVisible()) {
            prefUiFactory.activateVisiblePreferencesUi();
            return;
        }
        
        final PreferencesUI prefUI = prefUiFactory.createCurrentPreferencesUI();

        TmcCoreSettingsImpl settings = (TmcCoreSettingsImpl) TmcSettingsHolder.get();
        prefUI.setProjectDir(settings.getProjectRootDir());
        prefUI.setCheckForUpdatesInTheBackground(settings.isCheckingForUpdatesInTheBackground());
        prefUI.setCheckForUnopenedExercisesAtStartup(settings.isCheckingForUnopenedAtStartup());
        prefUI.setErrorMsgLocale(settings.getErrorMsgLocale());
        prefUI.setResolveProjectDependenciesEnabled(settings.getResolveDependencies());
        prefUI.setSendDiagnosticsEnabled(settings.getSendDiagnostics());

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
