package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.communication.TmcServerCommunicationTaskFactory;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;

import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import javax.swing.SwingUtilities;

public class UnlockExercisesAction {

    private CourseDb courseDb;
    private ConvenientDialogDisplayer dialogs;
    private ActionListener successListener;

    public UnlockExercisesAction() {
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

        // TODO: use core
        Callable<Void> task = new TmcServerCommunicationTaskFactory().getUnlockingTask(course);
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
                SwingUtilities.invokeLater(() -> {
                    dialogs.displayError("Failed to unlock exercises.", ex);
                });
            }
        });
    }
}
