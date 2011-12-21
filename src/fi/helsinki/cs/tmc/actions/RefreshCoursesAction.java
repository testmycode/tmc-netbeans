package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListenerList;
import java.util.List;

/**
 * Refreshes the course list in the background.
 */
public final class RefreshCoursesAction {
    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private ConvenientDialogDisplayer dialogs;
    
    private BgTaskListenerList<List<Course>> listeners;

    public RefreshCoursesAction() {
        this(TmcSettings.getDefault());
    }
    
    public RefreshCoursesAction(TmcSettings settings) {
        this.serverAccess = new ServerAccess(settings);
        this.serverAccess.setSettings(settings);
        this.courseDb = CourseDb.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();

        this.listeners = new BgTaskListenerList<List<Course>>();
    }

    public void addDefaultListener(boolean showDialogOnError, boolean updateCourseDb) {
        this.listeners.addListener(new DefaultListener(showDialogOnError, updateCourseDb));
    }

    public void addListener(BgTaskListener<List<Course>> listener) {
        this.listeners.addListener(listener);
    }

    public void run() {
        BgTask.start("Refreshing course list", serverAccess.getDownloadingCourseListTask(), listeners);
    }

    private class DefaultListener implements BgTaskListener<List<Course>> {
        private final boolean showDialogOnError;
        private final boolean updateCourseDb;

        public DefaultListener(boolean showDialogOnError, boolean updateCourseDb) {
            this.showDialogOnError = showDialogOnError;
            this.updateCourseDb = updateCourseDb;
        }

        @Override
        public void bgTaskReady(List<Course> result) {
            if (updateCourseDb) {
                courseDb.setAvailableCourses(result);
            }
        }

        @Override
        public void bgTaskCancelled() {
        }

        @Override
        public void bgTaskFailed(Throwable ex) {
            if (showDialogOnError) {
                dialogs.displayError("Course refresh failed.\n" + ex.getMessage());
            }
        }
    }
}
