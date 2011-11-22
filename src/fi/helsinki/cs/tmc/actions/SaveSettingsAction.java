package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

public class SaveSettingsAction extends AbstractAction {

    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;
    private OpenExercisesAction openExercisesAction;
    
    public SaveSettingsAction() {
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.openExercisesAction = new OpenExercisesAction();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!(e.getSource() instanceof PreferencesUI)) {
            throw new IllegalArgumentException(
                    SaveSettingsAction.class.getSimpleName()
                    + " expected event source to be a "
                    + PreferencesUI.class.getSimpleName());
        }

        PreferencesUI prefUi = (PreferencesUI) e.getSource();

        TmcSettings settings = TmcSettings.getSaved();
        settings.setUsername(prefUi.getUsername());
        settings.setPassword(prefUi.getPassword());
        settings.setSavingPassword(prefUi.getShouldSavePassword());
        settings.setServerBaseUrl(prefUi.getServerBaseUrl());
        settings.setProjectRootDir(prefUi.getProjectDir());
        if (prefUi.getSelectedCourse() != null) {
            String courseName = prefUi.getSelectedCourse().getName();
            courseDb.setCurrentCourseName(courseName);
            if (!courseDb.getCurrentCourseExercises().isEmpty()) {
                promptOpeningExercises();
            }
        } else {
            courseDb.setCurrentCourseName(null);
        }
        settings.save();
    }

    private void promptOpeningExercises() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (dialogs.askYesNo("Open latest exercises now?", "Open exercises?")) {
                    openExercisesAction.actionPerformed(null);
                }
            }
        });
    }
}
