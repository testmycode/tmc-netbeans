package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcServerAccess;
import fi.helsinki.cs.tmc.ui.PreferencesPanel;
import fi.helsinki.cs.tmc.utilities.ModalDialogDisplayer;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

public class SaveSettingsAction extends AbstractAction {

    private TmcServerAccess serverAccess;
    private LocalCourseCache localCourseCache;
    private ProjectMediator projectMediator;
    private ModalDialogDisplayer dialogDisplayer;
    
    public SaveSettingsAction() {
        this(TmcServerAccess.getDefault(),
                LocalCourseCache.getInstance(),
                ProjectMediator.getInstance(),
                ModalDialogDisplayer.getDefault());
    }

    public SaveSettingsAction(
            TmcServerAccess serverAccess,
            LocalCourseCache localCourseCache,
            ProjectMediator projectMediator,
            ModalDialogDisplayer dialogDisplayer) {
        this.serverAccess = serverAccess;
        this.localCourseCache = localCourseCache;
        this.projectMediator = projectMediator;
        this.dialogDisplayer = dialogDisplayer;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!(e.getSource() instanceof PreferencesPanel)) {
            throw new IllegalArgumentException(
                    SaveSettingsAction.class.getSimpleName()
                    + " expected event source to be a "
                    + PreferencesPanel.class.getSimpleName());
        }

        PreferencesPanel prefPanel = (PreferencesPanel) e.getSource();

        serverAccess.setUsername(prefPanel.getUsername());
        serverAccess.setBaseUrl(prefPanel.getServerBaseUrl());
        projectMediator.setProjectDir(prefPanel.getProjectDir());
        localCourseCache.setCurrentCourse(prefPanel.getSelectedCourse());
    }
}
