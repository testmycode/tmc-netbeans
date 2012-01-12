package fi.helsinki.cs.tmc.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.data.serialization.SubmissionResultParser;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.net.URI;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Cancellable;

/**
 * Sends a submission to the server and polls for results for a time.
 * 
 * Reports approximate progress.
 */
public class SubmissionResultWaiter implements CancellableCallable<SubmissionResult> {
    private static final Logger log = Logger.getLogger(SubmissionResultWaiter.class.getName());
    
    private final long DEFAULT_TIMEOUT = 180 * 1000;
    private final long DEFAULT_POLL_DELAY = 3 * 1000;
    
    private final URI submissionUrl;
    private final ProgressHandle progress;
    
    private final SubmissionResultParser resultParser;
    private final ServerAccess serverAccess;
    private final long timeout;
    private final long pollDelay;
    
    // Concurrency control on cancel
    private final Object lock = new Object();
    private boolean canceled = false;
    private Cancellable cancellableDownloadTask = null;

    public SubmissionResultWaiter(URI submissionUrl, ProgressHandle progress) {
        this.submissionUrl = submissionUrl;
        this.progress = progress;
        this.resultParser = new SubmissionResultParser();
        this.serverAccess = new ServerAccess();
        this.timeout = DEFAULT_TIMEOUT;
        this.pollDelay = DEFAULT_POLL_DELAY;
    }

    @Override
    public SubmissionResult call() throws Exception {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
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
                Thread.sleep(pollDelay);
            } else {
                return resultParser.parseFromJson(jsonText);
            }
        }
        throw new TimeoutException("Waiting for server timed out");
    }
    
    private boolean isProcessing(JsonElement responseRoot) {
        String status = responseRoot.getAsJsonObject().get("status").getAsString();
        return status.equals("processing");
    }
    
    private void updateProgress(JsonElement responseRoot) {
        int submissionsBefore = responseRoot.getAsJsonObject().get("submissions_before_this").getAsInt();
        if (submissionsBefore > 0) {
            progress.progress("Server is busy. Place in queue: " + (submissionsBefore + 1) + ".");
        } else {
            progress.progress("");
        }
    }

    @Override
    public boolean cancel() {
        synchronized (lock) {
            canceled = true;
            if (cancellableDownloadTask != null) {
                cancellableDownloadTask.cancel();
            }
        }
        return true;
    }
}
