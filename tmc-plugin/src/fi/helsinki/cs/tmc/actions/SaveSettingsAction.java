package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.LocalExerciseStatus;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.ui.DownloadOrUpdateExercisesDialog;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

public class SaveSettingsAction extends AbstractAction {

    private CourseDb courseDb;
    
    public SaveSettingsAction() {
        this.courseDb = CourseDb.getInstance();
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

        TmcSettings settings = TmcSettings.getDefault();
        settings.setUsername(prefUi.getUsername());
        settings.setPassword(prefUi.getPassword());
        settings.setSavingPassword(prefUi.getShouldSavePassword());
        settings.setServerBaseUrl(prefUi.getServerBaseUrl());
        settings.setProjectRootDir(prefUi.getProjectDir());
        settings.setCheckingForUpdatesInTheBackground(prefUi.getCheckForUpdatesInTheBackground());
        settings.setCheckingForUnopenedAtStartup(prefUi.getCheckForUnopenedExercisesAtStartup());
        settings.setIsSpywareEnabled(prefUi.getSpywareEnabled());
        settings.setErrorMsgLocale(prefUi.getErrorMsgLocale());
        
        if (prefUi.getSelectedCourse() != null) {
            String courseName = prefUi.getSelectedCourse().getName();
            courseDb.setAvailableCourses(prefUi.getAvailableCourses());
            courseDb.setCurrentCourseName(courseName);
            LocalExerciseStatus status = LocalExerciseStatus.get(courseDb.getCurrentCourseExercises());
            if (status.thereIsSomethingToDownload()) {
                DownloadOrUpdateExercisesDialog.display(status.downloadable, status.updateable);
            }
        } else {
            courseDb.setCurrentCourseName(null);
        }
        
        settings.save();
    }
}
