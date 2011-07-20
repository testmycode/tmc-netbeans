package fi.helsinki.cs.tmc.utilities.threading;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Cancellable;
import org.openide.util.RequestProcessor;
import org.openide.util.TaskListener;

/**
 * This class is used to start a task to run in its own thread.
 * It uses ProgressHandle to display an indicator on the bottom of the NetBeans
 * window to indicate that a task is running.
 * 
 * The class can be used for any kinds of tasks but Palikka uses it for
 * downloads only.
 * 
 * @author jmturpei
 */
@Deprecated
public class LegacyBackgroundWorker {

    /**
     * Used to execute tasks in their own thread.
     * See NetBeans api for more information.
     */
    private final static RequestProcessor rp = new RequestProcessor("tasks", 1, true);

    /**
     * This method is used to execute a given task using NetBeans
     * @param task The task that we want to execute
     * @param displayMsg The message to display in the progress indicator.
     * @param cancellable true if this task can be cancelled and false if not.
     */
    public void startTask(final LegacyTaskWithProgressIndicator task, String displayMsg, boolean cancellable) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }
        if (displayMsg == null) {
            displayMsg = "";
        }

        final RequestProcessor.Task runnableTask = rp.create(task);

        final ProgressHandle progressHandle;

        if (cancellable) {


            progressHandle = ProgressHandleFactory.createHandle(displayMsg, new Cancellable() {

                @Override
                public boolean cancel() {
                    if (runnableTask != null) {
                        task.cancel(true);
                        return true;
                    }
                    return false;
                }
            });

        } else {
            progressHandle = ProgressHandleFactory.createHandle(displayMsg);
        }




        task.setProgressHandle(progressHandle);

        runnableTask.addTaskListener(new TaskListener() {

            @Override
            public void taskFinished(org.openide.util.Task task) {
                progressHandle.finish();
            }
        });

        runnableTask.schedule(0);
    }
}
