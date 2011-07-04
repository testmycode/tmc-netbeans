package palikka.utilities.http;

import java.io.InputStream;
import palikka.utilities.threading.BackgroundWorker;
import palikka.utilities.threading.ITaskListener;
import palikka.utilities.threading.TaskWithProgressIndicator;

/**
 * This class is used to upload a file using the HTML POST method.
 * @author jmturpei
 */
public class FileUploaderAsync implements ITaskListener {

    /**
     * The uploader that does the actual work. It operates in its own thread.
     */
    private volatile FileUploader uploader;
    
    /**
     * The worker can run tasks in dedicated threads.
     */
    private BackgroundWorker worker;
    
    /**
     * This provides the task and a progress indicator (courtesy of NetBeans)
     * to the BackgroundWorker.
     */
    private TaskWithProgressIndicator task;
    
    /*
     * A few booleans to indicate the status of the upload task.
     */
    private boolean taskStarted = false;
    private boolean taskEnded = false;
    
    /**
     * This is checked from within the code to check wether the upload has
     * been cancelled.
     */
    private boolean cancelUpload = false;
    
    /**
     * The interface that listens to this FileUploaderAsync.
     * The interface's methods are called when the task finishes or fails.
     */
    private IUploadListener listener;
    
    /**
     * Used to store the error message that comes from the FileUploader.
     */
    private String errorMsg = "";

    private FileUploaderAsync() {
    }

    /**
     * Constructor. No files are sent before the send() method is invoked.
     * @param serverAddress The address where the file should be uploaded to.
     * @param listener The interface that listens to this FileUploaderAsync.
     * @throws NullPointerException If no listener was provided.
     */
    public FileUploaderAsync(String serverAddress, IUploadListener listener) throws NullPointerException {
        if (listener == null) {
            throw new NullPointerException("listener is null");
        }

        uploader = new FileUploader(serverAddress);
        this.listener = listener;

    }

    /**
     * Tell this FileUploaderAsync to send all files attached to its uploader.
     * @param displayMsg The message to display at the bottom of the window
     * in the NetBeans' progress indicator.
     */
    public void send(String displayMsg) {

        if (taskStarted) {
            return;
        }
        taskStarted = true;

        worker = new BackgroundWorker();
        task = new TaskWithProgressIndicator(this) {

            @Override
            public void executeTask() throws Exception {

                uploader.send();

            }
        };

        worker.startTask(task, displayMsg, false);

    }

    /**
     * Add a key-value pair to the form being sent (HTML POST).
     * This is used to add the student ID to the form which the server then
     * reads.
     * @param key Field name
     * @param value Field value
     */
    public void AddStringKeyValuePart(String key, String value) {
        uploader.AddStringKeyValuePart(key, value);
    }

    /**
     * Add a file to the uploader. Palikka only sends a single file but the
     * uploader allows for multiple files to be sent.
     * @param fileContent The file to send in byte[] form
     * @param filename The name of the file to be sent
     * @param formKey The key for the file in the POST form
     */
    public void AddFile(byte[] fileContent, String filename, String formKey) {
        uploader.AddFile(fileContent, filename, formKey);
    }

    /**
     * Sets the timeout for the uploader given as ms.
     * Basically how long the uploader tries to send the file without success
     * before interrupting.
     * @param timeout 
     */
    public void setTimeout(int timeout) {
        uploader.setTimeout(timeout);
    }

    /**
     * Fetch the error message if the upload was completed.
     * If the task is still running throws and IllegalStateException.
     * @return The error message as String
     */
    public String getErrorMsg() {
        if (!taskEnded) {
            throw new IllegalStateException("upload isn't completed yet");
        }
        return errorMsg;
    }

    /**
     * Used to route the jsonLink from FileUploader
     * @return 
     */
    public InputStream getResponse() {
        return this.uploader.getResponse();
    }

    /**
     * Called by the uploader when the task is finished.
     */
    @Override
    public void taskFinished() {
        taskEnded = true;
        if (cancelUpload) {
            return;
        }

        listener.uploadCompleted(this);
    }

    /**
     * Called by the uploader when the task fails.
     * @param errorMsg 
     */
    @Override
    public void taskFailed(String errorMsg) {
        taskEnded = true;
        this.errorMsg = errorMsg;
        if (cancelUpload) {
            return;
        }

        listener.uploadFailed(this);
    }

    /**
     * Called by the uploader if the user aborts the upload.
     */
    @Override
    public void taskCancelledByUser() {
    }
}
