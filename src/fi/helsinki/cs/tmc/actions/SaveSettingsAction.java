package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.utilities.ConvenientDialogDisplayer;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

public class SaveSettingsAction extends AbstractAction {

    private ServerAccess serverAccess;
    private LocalCourseCache localCourseCache;
    private ProjectMediator projectMediator;
    private ConvenientDialogDisplayer dialogs;
    private OpenExercisesAction openExercisesAction;
    
    public SaveSettingsAction() {
        this(ServerAccess.getDefault(),
                LocalCourseCache.getInstance(),
                ProjectMediator.getInstance(),
                ConvenientDialogDisplayer.getDefault(),
                new OpenExercisesAction());
    }

    public SaveSettingsAction(
            ServerAccess serverAccess,
            LocalCourseCache localCourseCache,
            ProjectMediator projectMediator,
            ConvenientDialogDisplayer dialogs,
            OpenExercisesAction openExercisesAction) {
        this.serverAccess = serverAccess;
        this.localCourseCache = localCourseCache;
        this.projectMediator = projectMediator;
        this.dialogs = dialogs;
        this.openExercisesAction = openExercisesAction;
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

        serverAccess.setUsername(prefUi.getUsername());
        serverAccess.setBaseUrl(prefUi.getServerBaseUrl());
        projectMediator.setProjectRootDir(prefUi.getProjectDir());
        if (prefUi.getSelectedCourse() != null) {
            String courseName = prefUi.getSelectedCourse().getName();
            localCourseCache.setCurrentCourseName(courseName);
            promptOpeningExercises();
        } else {
            localCourseCache.setCurrentCourseName(null);
        }
    }

    private void promptOpeningExercises() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (dialogs.askYesNo("Shall I open the current exercises?", "Open exercises?")) {
                    openExercisesAction.actionPerformed(null);
                }
            }
        });
    }
}
