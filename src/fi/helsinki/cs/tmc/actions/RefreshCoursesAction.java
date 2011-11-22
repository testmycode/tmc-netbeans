package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.ui.PreferencesUIFactory;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

public class RefreshCoursesAction extends AbstractAction {
    private TmcSettings settings;
    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private PreferencesUIFactory prefUIFactory;
    private ConvenientDialogDisplayer dialogs;

    public RefreshCoursesAction() {
        this(TmcSettings.getSaved());
    }
    
    public RefreshCoursesAction(TmcSettings settings) {
        this.settings = TmcSettings.getSaved();
        this.serverAccess = ServerAccess.create();
        this.serverAccess.setSettings(settings);
        this.courseDb = CourseDb.getInstance();
        this.prefUIFactory = PreferencesUIFactory.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (settings.getServerBaseUrl() == null || settings.getServerBaseUrl().trim().isEmpty()) {
            dialogs.displayError("Please set the server address first.");
            notifyPrefUiThatCourseRefreshFailed();
            return;
        }
        
        serverAccess.startDownloadingCourseList(new BgTaskListener<CourseList>() {
            @Override
            public void bgTaskReady(CourseList result) {
                courseDb.setAvailableCourses(result);
                PreferencesUI prefUi = prefUIFactory.getCurrentUI();
                if (prefUi != null) {
                    prefUi.setAvailableCourses(result);
                }
            }

            @Override
            public void bgTaskCancelled() {
                notifyPrefUiThatCourseRefreshFailed();
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                dialogs.displayError("Course refresh failed.\n" + ex.getMessage());
                notifyPrefUiThatCourseRefreshFailed();
            }
        });
    }
    
    private void notifyPrefUiThatCourseRefreshFailed() {
        PreferencesUI prefUi = prefUIFactory.getCurrentUI();
        if (prefUi != null) {
            prefUi.courseRefreshFailedOrCanceled();
        }
    }
    
}
