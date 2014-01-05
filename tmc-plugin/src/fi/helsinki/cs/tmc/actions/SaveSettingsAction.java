package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.LocalExerciseStatus;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.ui.DownloadOrUpdateExercisesDialog;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import java.awt.event.ActionEvent;
import java.util.List;
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

        if (prefUi.getSelectedCourseName() != null) {
            courseDb.setAvailableCourses(prefUi.getAvailableCourses());
            courseDb.setCurrentCourseName(prefUi.getSelectedCourseName());

            RefreshCoursesAction refresh = new RefreshCoursesAction();
            refresh.addDefaultListener(true, true);
            refresh.addListener(new BgTaskListener<List<Course>>() {
                @Override
                public void bgTaskReady(List<Course> result) {
                    LocalExerciseStatus status = LocalExerciseStatus.get(courseDb.getCurrentCourseExercises());
                    if (status.thereIsSomethingToDownload(false)) {
                        DownloadOrUpdateExercisesDialog.display(status.unlockable, status.downloadableUncompleted, status.updateable);
                    }
                }

                @Override
                public void bgTaskCancelled() {
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                }
            });
            refresh.run();
        }
        
        settings.save();
    }
}
