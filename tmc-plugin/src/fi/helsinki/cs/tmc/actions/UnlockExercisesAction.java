package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.awt.event.ActionListener;

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
                if (successListener != null) {
                    successListener.actionPerformed(null);
                }
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
}
