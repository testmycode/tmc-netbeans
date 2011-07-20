package fi.helsinki.cs.tmc.utilities.threading;

/**
 * An interface for listening for a task to report in when it has finished or
 * failed.
 * @author jmturpei
 */
public interface ITaskListener {

    public void taskFinished();

    public void taskCancelled();

    public void taskFailed(String errorMsg);
}
