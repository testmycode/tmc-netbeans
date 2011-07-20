package fi.helsinki.cs.tmc.utilities.threading;

/**
 * An interface for listening for a background task to report in when it has
 * finished or failed.
 * 
 * NOTE: All methods are to be invoked in the swing thread!
 */
@Deprecated
public interface ITaskListener {

    public void taskFinished();

    public void taskCancelled();

    public void taskFailed(String errorMsg);
}
