package fi.helsinki.cs.tmc.utilities;

import com.google.common.util.concurrent.Futures;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openide.util.RequestProcessor;

/**
 * A task that can be started repeatedly, but ensures only one instance is running at a time.
 */
public class SingletonTask {
    private RequestProcessor requestProcessor;
    private Runnable runnable;
    private Future<?> task;
    private ScheduledFuture<?> autostartTask = null;

    public SingletonTask(Runnable runnable) {
        this(runnable, TmcRequestProcessor.instance);
    }

    public SingletonTask(Runnable runnable, RequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
        this.runnable = runnable;
        this.task = Futures.immediateFuture(null);
    }

    public synchronized void setInterval(long delay) {
        unsetInterval();
        autostartTask = requestProcessor.scheduleWithFixedDelay(autostartRunnable, delay, delay, TimeUnit.MILLISECONDS);
    }

    public synchronized void unsetInterval() {
        if (autostartTask != null) {
            autostartTask.cancel(true);
            autostartTask = null;
        }
    }

    private final Runnable autostartRunnable = new Runnable() {
        @Override
        public void run() {
            start();
        }
    };

    /**
     * Starts the task unless it's already running.
     */
    public synchronized void start() {
        if (task.isDone()) {
            task = requestProcessor.submit(runnable);
        }
    }

    /**
     * Waits for the task to finish if it is currently running.
     *
     * Note: this method does not indicate in any way whether the task succeeded or failed.
     *
     * @param timeout Maximum time in milliseconds to wait before throwing a TimeoutException.
     */
    public synchronized void waitUntilFinished(long timeout) throws TimeoutException, InterruptedException {
        try {
            task.get(timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex) {
            // Ignore
        }
    }

    public synchronized boolean isRunning() {
        return !task.isDone();
    }
}
