package fi.helsinki.cs.tmc.utilities.http;

import java.io.InputStream;
import fi.helsinki.cs.tmc.utilities.threading.BackgroundWorker;
import fi.helsinki.cs.tmc.utilities.threading.ITaskListener;
import fi.helsinki.cs.tmc.utilities.threading.TaskWithProgressIndicator;

/**
 * This class is used to download files from a server.
 * @author jmturpei
 */
public class FileDownloaderAsync implements ITaskListener {

    /**
     * The downloader that actually does all the work
     */
    private volatile FileDownloader downloader;
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
     * A few booleans to indicate the status of the download task.
     */
    private boolean taskStarted = false;
    private boolean taskEnded = false;
    /**
     * This is checked from within the code to check wether the download has
     * been cancelled.
     */
    private boolean cancelDownload = false;
    /**
     * The interface that listens to this FileDownloaderAsync.
     * The interface's methods are called when the task finishes or fails.
     */
    private IDownloadListener listener;
    /**
     * Used to store the error message that comes from the FileDownloader.
     */
    private String errorMsg = "";

    /**
     * Constructor
     * @param downloadAddress The address where we should download the file from.
     * @param listener The interface that listens to this FileDownloaderAsync
     * @throws NullPointerException If no listener is provided
     * @throws Exception When this fails to create a FileDownloader
     */
    public FileDownloaderAsync(String downloadAddress, IDownloadListener listener) throws NullPointerException, Exception {
        if (listener == null) {
            throw new NullPointerException("listener is null");
        }

        downloader = new FileDownloader(downloadAddress);
        this.listener = listener;
    }

    /**
     * Tell this FileDownloaderAsync to start the download
     * @param displayMsg Message to display when downloading
     */
    public void download(String displayMsg) {

        if (taskStarted) {
            return;
        }
        taskStarted = true;

        worker = new BackgroundWorker();
        task = new TaskWithProgressIndicator(this) {

            @Override
            public void executeTask() throws Exception {
                downloader.download();
            }
        };

        worker.startTask(task, displayMsg, true);
    }

    /**
     * Set the timeout for the downloader
     * @param timeout 
     */
    public void setTimeout(int timeout) {
        downloader.setTimeout(timeout);
    }

    /**
     * 
     * @return The error message if the download has finished
     */
    public String getErrorMsg() {
        if (!taskEnded) {
            throw new IllegalStateException("download hasn't completed yet");
        }
        return errorMsg;
    }

    /**
     * 
     * @return InputStream to the file downloaded by the downloader if the download has finished
     */
    public InputStream getFileContent() {
        if (!taskEnded) {
            throw new IllegalStateException("download hasn't completed yet");
        }

        return downloader.getFileContent();
    }

    /**
     * Cancel this download
     */
    public void cancel() {
        cancelDownload = true;
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * Called by the downloader when the download is completed
     */
    @Override
    public void taskFinished() {
        taskEnded = true;
        if (cancelDownload) {
            return;
        }

        listener.downloadCompleted(this);

    }

    /**
     * Called by the downloader if the user aborts the download
     */
    @Override
    public void taskCancelled() {
        taskEnded = true;
        cancelDownload = true;

        listener.downloadCancelledByUser(this);
    }

    /**
     * Called by the downloader if the download fails eg. timeout.
     * @param errorMsg 
     */
    @Override
    public void taskFailed(String errorMsg) {
        taskEnded = true;
        this.errorMsg = errorMsg;
        if (cancelDownload) {
            return;
        }

        listener.downloadFailed(this);
    }
}
