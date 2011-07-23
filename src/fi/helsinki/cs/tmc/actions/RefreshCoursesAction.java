package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.model.LocalCourseCache;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.ui.PreferencesUI;
import fi.helsinki.cs.tmc.ui.PreferencesUIFactory;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.ModalDialogDisplayer;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

public class RefreshCoursesAction extends AbstractAction {
    private ServerAccess serverAccess;
    private LocalCourseCache localCourseCache;
    private PreferencesUIFactory prefUIFactory;
    private ModalDialogDisplayer dialogs;

    public RefreshCoursesAction() {
        this(ServerAccess.getDefault(),
                LocalCourseCache.getInstance(),
                PreferencesUIFactory.getInstance(),
                ModalDialogDisplayer.getDefault());
    }

    public RefreshCoursesAction(
            ServerAccess serverAccess,
            LocalCourseCache localCourseCache,
            PreferencesUIFactory prefUiFactory,
            ModalDialogDisplayer dialogs) {
        this.serverAccess = serverAccess;
        this.localCourseCache = localCourseCache;
        this.prefUIFactory = prefUiFactory;
        this.dialogs = dialogs;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ensureLatestBaseUrlSaved(); // not ideal
        
        serverAccess.startDownloadingCourseList(new BgTaskListener<CourseCollection>() {
            @Override
            public void backgroundTaskReady(CourseCollection result) {
                localCourseCache.setAvailableCourses(result);
                PreferencesUI prefUi = prefUIFactory.getCurrentUI();
                if (prefUi != null) {
                    prefUi.setAvailableCourses(result);
                }
            }

            @Override
            public void backgroundTaskCancelled() {
                notifyPrefUiThatCourseRefreshFailed();
            }

            @Override
            public void backgroundTaskFailed(Throwable ex) {
                dialogs.displayError("Course refresh failed.\n" + ex.getMessage());
                notifyPrefUiThatCourseRefreshFailed();
            }
        });
    }

    private void ensureLatestBaseUrlSaved() {
        PreferencesUI prefUi = prefUIFactory.getCurrentUI();
        if (prefUi != null) {
            serverAccess.setBaseUrl(prefUi.getServerBaseUrl());
        }
    }
    
    private void notifyPrefUiThatCourseRefreshFailed() {
        PreferencesUI prefUi = prefUIFactory.getCurrentUI();
        if (prefUi != null) {
            prefUi.courseRefreshFailedOrCanceled();
        }
    }
    
}
