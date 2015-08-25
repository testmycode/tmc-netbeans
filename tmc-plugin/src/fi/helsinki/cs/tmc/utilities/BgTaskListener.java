package fi.helsinki.cs.tmc.utilities;

/**
 * Gets notified in the Swing thread when a {@link BackgroundTask} is finished.
 */
public interface BgTaskListener<V> {

    /**
     * Called with the result of the background task upon successful completion.
     * @param result The result of the background task.
     */
    public void bgTaskReady(V result);

    /**
     * Called if the background task was cancelled by the user or programmatically.
     */
    public void bgTaskCancelled();

    /**
     * Called if the runnable threw an exception.
     *
     * @param e The exception that caused the task to fail.
     */
    public void bgTaskFailed(Throwable ex);
}
