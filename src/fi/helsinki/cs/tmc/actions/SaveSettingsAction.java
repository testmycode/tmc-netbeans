package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcServerAccess;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

public class SaveSettingsAction extends AbstractAction {

    private TmcServerAccess serverAccess;
    private LocalCourseCache localCourseCache;
    private ProjectMediator projectMediator;
    
    public SaveSettingsAction() {
        this(TmcServerAccess.getDefault(),
                LocalCourseCache.getInstance(),
                ProjectMediator.getInstance());
    }

    public SaveSettingsAction(
            TmcServerAccess serverAccess,
            LocalCourseCache localCourseCache,
            ProjectMediator projectMediator) {
        this.serverAccess = serverAccess;
        this.localCourseCache = localCourseCache;
        this.projectMediator = projectMediator;
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
        projectMediator.setProjectDir(prefUi.getProjectDir());
        localCourseCache.setCurrentCourse(prefUi.getSelectedCourse());
    }
}
