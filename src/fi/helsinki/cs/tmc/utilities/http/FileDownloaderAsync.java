package fi.helsinki.cs.tmc.utilities.http;

import java.io.InputStream;
import fi.helsinki.cs.tmc.utilities.threading.LegacyBackgroundWorker;
import fi.helsinki.cs.tmc.utilities.threading.ITaskListener;
import fi.helsinki.cs.tmc.utilities.threading.LegacyTaskWithProgressIndicator;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * This class is used to download files from a server.
 * @author jmturpei
 */
@Deprecated
public class FileDownloaderAsync implements ITaskListener {

    
    private String downloadAddress;
    
    private byte[] result;
    
    /**
     * The worker can run tasks in dedicated threads.
     */
    private LegacyBackgroundWorker worker;
    /**
     * This provides the task and a progress indicator (courtesy of NetBeans)
     * to the LegacyBackgroundWorker.
     */
    private LegacyTaskWithProgressIndicator task;
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

        this.downloadAddress = downloadAddress;
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

        worker = new LegacyBackgroundWorker();
        task = new LegacyTaskWithProgressIndicator(this) {

            @Override
            public void executeTask() throws IOException, InterruptedException {
                HttpRequestExecutor download = new HttpRequestExecutor(downloadAddress);
                result = download.call();
            }
        };

        worker.startTask(task, displayMsg, true);
    }

    /**
     * Set the timeout for the downloader
     * @param timeout 
     */
    @Deprecated
    public void setTimeout(int timeout) {
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

        return new ByteArrayInputStream(result);
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
