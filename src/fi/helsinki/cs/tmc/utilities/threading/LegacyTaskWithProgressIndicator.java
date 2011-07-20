package fi.helsinki.cs.tmc.utilities.threading;

import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;

/**
 * This class can be used to create a runnable task which can be then given
 * to BackGroundWorker for execution.
 * @author jmturpei
 */
public abstract class LegacyTaskWithProgressIndicator implements Runnable {

    /**
     * Allows the user to monitor the progress of this task.
     * See NetBeans' api for more information.
     */
    private volatile ProgressHandle progressHandle;
    
    /**
     * This value is checked within the class to see if the task has been cancelled.
     */
    private volatile boolean isCancelled = false;
    
    /**
     * The interface listening to this task.
     */
    private volatile ITaskListener listener;

    /**
     * Constructor
     * @param listener Becomes the interface that listens to this task.
     * Upon completion or failure this task calls a method in the listener.
     */
    public LegacyTaskWithProgressIndicator(ITaskListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener is null");
        }

        this.listener = listener;
    }

    /**
     * Call this to run the task.
     */
    @Override
    public void run() {
        try {
            progressHandle.setInitialDelay(0);
            progressHandle.start();
            progressHandle.switchToIndeterminate();

            if (isCancelled()) {
                return;
            }

            executeTask();
            if (isCancelled()) {
                return;
            }

        } catch (Exception e) {

            final String errorMsg = e.getMessage();

            if (!isCancelled()) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        listener.taskFailed(errorMsg);
                    }
                });
            }

            return;
        }



        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                listener.taskFinished();
            }
        });

    }

    /**
     * Set the message to display in NetBeans.
     * @param displayMsg 
     */
    protected synchronized void setDisplayMsg(String displayMsg) {
        if (progressHandle == null) {
            throw new IllegalStateException("Title cannot be set before progress handle is set");
        }

        progressHandle.setDisplayName(displayMsg);
    }

    /**
     * Sets this task's progressHandle that monitors the progress of this task.
     * @param progressHandle 
     */
    public synchronized void setProgressHandle(ProgressHandle progressHandle) {
        if (progressHandle == null) {
            throw new NullPointerException("ProgressHandle is null");
        }
        this.progressHandle = progressHandle;
    }

    /**
     * 
     * @return true if this task is cancelled and false if not.
     */
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Cancels this task.
     * @param invokeListener true if we should inform the listener and false if we should not.
     */
    public synchronized void cancel(boolean invokeListener) {
        if (isCancelled()) {
            return;
        }

        isCancelled = true;


        if (invokeListener) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    listener.taskCancelled();
                }
            });
        }

        if (progressHandle != null) {
            progressHandle.finish();
        }

    }

    /**
     * Basically the same as run(). This is implemented as needed.
     * @throws Exception 
     */
    public abstract void executeTask() throws Exception;
}
