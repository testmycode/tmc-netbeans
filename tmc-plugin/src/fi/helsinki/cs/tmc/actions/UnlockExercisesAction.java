package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.awt.event.ActionListener;
import java.util.List;

public class UnlockExercisesAction {
    private ServerAccess server;
    private CourseDb courseDb;
    private ConvenientDialogDisplayer dialogs;
    private ActionListener successListener;

    public UnlockExercisesAction() {
        this.server = new ServerAccess();
        this.courseDb = CourseDb.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
    }

    public void setSuccessListener(ActionListener successListener) {
        this.successListener = successListener;
    }
    
    public void run() {
        Course course = courseDb.getCurrentCourse();
        if (course == null) {
            return;
        }
        
        CancellableCallable<Void> task = server.getUnlockingTask(course);
        BgTask.start("Unlocking exercises", task, new BgTaskListener<Void>() {
            @Override
            public void bgTaskReady(Void result) {
                updateCourseList();
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                dialogs.displayError("Failed to unlock exercises.", ex);
            }
        });
    }
    
    private void updateCourseList() {
        BgTask.start("Refreshing exercise status", server.getDownloadingCourseListTask(), new BgTaskListener<List<Course>>() {
            @Override
            public void bgTaskReady(List<Course> result) {
                courseDb.setAvailableCourses(result);
                if (successListener != null) {
                    successListener.actionPerformed(null);
                }
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                dialogs.displayError("Failed to refresh exercise status after unlocking exercises.\nPlease try again (TMC -> Check for new exercises / updates).", ex);
            }
        });
    }
}
