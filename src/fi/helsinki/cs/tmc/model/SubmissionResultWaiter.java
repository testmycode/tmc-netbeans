package fi.helsinki.cs.tmc.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.data.serialization.SubmissionResultParser;
import fi.helsinki.cs.tmc.ui.SubmissionProgressView;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.util.logging.Logger;
import org.openide.util.Cancellable;

/**
 * Sends a submission to the server and polls for results for a time.
 * 
 * Reports approximate progress.
 */
public class SubmissionResultWaiter implements CancellableCallable<SubmissionResult> {
    private static final Logger log = Logger.getLogger(SubmissionResultWaiter.class.getName());
    
    private final long DEFAULT_POLL_DELAY = 3 * 1000;
    
    private final String submissionUrl;
    private final SubmissionProgressView view;
    
    private final SubmissionResultParser resultParser;
    private final ServerAccess serverAccess;
    private final long pollDelay;
    
    // Concurrency control on cancel
    private final Object lock = new Object();
    private boolean canceled = false;
    private Cancellable cancellableDownloadTask = null;
    private Thread sleepingThread = null;

    public SubmissionResultWaiter(String submissionUrl, SubmissionProgressView view) {
        this.submissionUrl = submissionUrl;
        this.view = view;
        this.resultParser = new SubmissionResultParser();
        this.serverAccess = new ServerAccess();
        this.pollDelay = DEFAULT_POLL_DELAY;
    }

    @Override
    public SubmissionResult call() throws Exception {
        while (true) {
            CancellableCallable<String> downloadTask = serverAccess.getSubmissionFetchJob(submissionUrl);
            
            synchronized (lock) {
                if (canceled) {
                    String msg = "Waiting for submission results cancelled";
                    log.info(msg);
                    throw new InterruptedException(msg);
                }
                cancellableDownloadTask = downloadTask;
            }
            
            log.info("Requesting submission results");
            String jsonText = downloadTask.call();
            JsonElement json = new JsonParser().parse(jsonText);
            
            if (isProcessing(json)) {
                updateProgress(json);
                sleepInterruptably(pollDelay);
            } else {
                return resultParser.parseFromJson(jsonText);
            }
        }
    }
    
    private void sleepInterruptably(long delay) {
        synchronized (lock) {
            if (canceled) {
                return;
            }
            sleepingThread = Thread.currentThread(); // Prepare to receive interrupt.
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
        }
        synchronized (lock) {
            sleepingThread = null;
            Thread.interrupted(); // Consume any interrupt.
        }
    }
    
    private boolean isProcessing(JsonElement responseRoot) {
        String status = responseRoot.getAsJsonObject().get("status").getAsString();
        return status.equals("processing");
    }
    
    private void updateProgress(JsonElement responseRoot) {
        int submissionsBefore = responseRoot.getAsJsonObject().get("submissions_before_this").getAsInt();
        view.setPositionInQueueFromAnyThread(submissionsBefore + 1);
    }

    @Override
    public boolean cancel() {
        /*
         * One of three conditions always hold:
         * 1. cancellableDownloadTask is set to an active download task.
         * 2. sleepingThread is set and may be sent an interrupt.
         * 3. canceled will be checked soon.
         */
        synchronized (lock) {
            canceled = true;
            if (cancellableDownloadTask != null) {
                cancellableDownloadTask.cancel();
            } else if (sleepingThread != null) {
                sleepingThread.interrupt();
            }
        }
        return true;
    }
}
