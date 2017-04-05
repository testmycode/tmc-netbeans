package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;
import fi.helsinki.cs.tmc.core.events.TmcEvent;
import fi.helsinki.cs.tmc.core.events.TmcEventBus;
import fi.helsinki.cs.tmc.core.utilities.TmcServerAddressNormalizer;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.LocalExerciseStatus;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.ui.DownloadOrUpdateExercisesDialog;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;

import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;

public class SaveSettingsAction extends AbstractAction {

    private CourseDb courseDb;
    private TmcEventBus eventBus;
    private TmcCore tmcCore;
    private final FixUnoptimalSettings fixUnoptimalSettings;
    private SendDiagnostics sendDiagnostics;
    
    public SaveSettingsAction() {
        this.courseDb = CourseDb.getInstance();
        this.eventBus = TmcEventBus.getDefault();
        this.tmcCore = TmcCore.get();
        this.fixUnoptimalSettings = new FixUnoptimalSettings();
        this.sendDiagnostics = new SendDiagnostics();
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

        TmcCoreSettingsImpl settings = (TmcCoreSettingsImpl) TmcSettingsHolder.get();
        
        settings.setUsername(prefUi.getUsername());
        settings.setServerBaseUrl(prefUi.getServerBaseUrl());
        TmcServerAddressNormalizer.normalize();
        settings.setProjectRootDir(prefUi.getProjectDir());
        settings.setCheckingForUpdatesInTheBackground(prefUi.getCheckForUpdatesInTheBackground());
        settings.setCheckingForUnopenedAtStartup(prefUi.getCheckForUnopenedExercisesAtStartup());
        settings.setIsSpywareEnabled(prefUi.getSpywareEnabled());
        settings.setErrorMsgLocale(prefUi.getErrorMsgLocale());
        settings.setResolveDependencies(prefUi.getResolveProjectDependenciesEnabled());
        settings.setSendDiagnostics(prefUi.getSendDiagnosticsEnabled());

        eventBus.post(new InvokedEvent());
        
        if (settings.getResolveDependencies()) {
            fixUnoptimalSettings.run();
        } else {
            fixUnoptimalSettings.undo();
        }

        if (settings.getSendDiagnostics()) {
            sendDiagnostics.run();
        }

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

    public static class InvokedEvent implements TmcEvent {}
}
