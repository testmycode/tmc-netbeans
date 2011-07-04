package palikka.utilities.threading;

/**
 * An interface for listening for a task to report in when it has finished or
 * failed.
 * @author jmturpei
 */
public interface ITaskListener {

    public void taskFinished();

    public void taskCancelledByUser();

    public void taskFailed(String errorMsg);
}
