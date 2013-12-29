package fi.helsinki.cs.tmc.spyware;

import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Buffers {@link LoggableEvent}s and sends them to the server periodically.
 */
public class EventSender implements EventReceiver {
    private static final Logger log = Logger.getLogger(EventSender.class.getName());

    public static long DEFAULT_DELAY = 5*60*1000;
    public static int DEFAULT_MAX_EVENTS = 64 * 1024;

    private SpywareSettings settings;
    private ServerAccess serverAccess;

    private long delay = DEFAULT_DELAY;
    private int maxEvents = DEFAULT_MAX_EVENTS;

    private ArrayList<LoggableEvent> buffer;
    private java.util.Timer sendTimer;

    public EventSender(SpywareSettings settings, ServerAccess serverAccess) {
        this.settings = settings;
        this.serverAccess = serverAccess;
        this.buffer = new ArrayList<LoggableEvent>();
        this.sendTimer = new java.util.Timer("EventSender timer", true);
        this.sendTimer.schedule(sendTask, delay, delay);
    }

    public synchronized void sendNow() {
        sendTask.run();
    }

    public synchronized void waitUntilCurrentSendingFinished(long timeout) throws InterruptedException {
        sendTask.waitUntilFinished(timeout);
    }

    @Override
    public synchronized void receiveEvent(LoggableEvent event) {
        if (!settings.isSpywareEnabled()) {
            return;
        }
        buffer.add(event);
        removeIfOverLimit();
    }

    public synchronized ArrayList<LoggableEvent> takeEvents() {
        return takeEvents(buffer.size());
    }

    public synchronized ArrayList<LoggableEvent> takeEvents(int limit) {
        limit = Math.min(limit, buffer.size());
        ArrayList<LoggableEvent> result = new ArrayList<LoggableEvent>(buffer.subList(0, limit));
        buffer.subList(0, limit).clear();
        return result;
    }

    public synchronized void prependEvents(List<LoggableEvent> events) {
        buffer.addAll(0, events);
        removeIfOverLimit();
    }

    private void removeIfOverLimit() {
        if (buffer.size() > maxEvents) {
            buffer.subList(0, buffer.size() - maxEvents).clear();
        }
    }

    private class SendTask extends TimerTask {
        // Sending too many at once may go over the server's POST size limit.
        private static final int MAX_EVENTS_PER_SEND = 500;

        private final Object doneCondVar = new Object();
        private volatile boolean running = false;
        private boolean moreToSend = false;

        // run() is synchronized because it may be called by the timer as well as sendNow().
        @Override
        public synchronized void run() {
            synchronized (doneCondVar) {
                running = true;
            }

            try {
                do {
                    doSend();
                } while (moreToSend);
            } finally {
                synchronized (doneCondVar) {
                    running = false;
                    doneCondVar.notifyAll();
                }
            }
        }

        private void doSend() {
            final List<LoggableEvent> events = takeEvents(MAX_EVENTS_PER_SEND);
            moreToSend = events.size() == MAX_EVENTS_PER_SEND;
            if (events.isEmpty()) {
                return;
            }

            log.log(Level.INFO, "Sending {0} events", events.size());

            CancellableCallable<Object> task = serverAccess.getSendEventLogJob(events);
            // If we fail, we add the events back to be tried again later
            Future<Object> future = BgTask.start("Sending stats", task, new BgTaskListener<Object>() {
                @Override
                public void bgTaskReady(Object result) {
                    log.info("Events sent");
                }

                @Override
                public void bgTaskCancelled() {
                    prependEvents(events);
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                    log.log(Level.INFO, "Sending events failed", ex);
                    prependEvents(events);
                }
            });

            // Only permit one sending task to exist at once.
            // Timer will not call timerTask before the previous task returns.
            try {
                future.get();
            } catch (InterruptedException ex) {
                future.cancel(true);
            } catch (ExecutionException ex) {
                throw new RuntimeException(ex.getCause());
            }
        }

        public void waitUntilFinished(long timeout) throws InterruptedException {
            synchronized (doneCondVar) {
                if (running) {
                    doneCondVar.wait(timeout);
                }
            }
        }
    }

    private final SendTask sendTask = new SendTask();

    /**
     * Stops sending any more events.
     *
     * Buffer manipulation methods may still be called.
     */
    @Override
    public void close() {
        sendTask.cancel();
        try {
            sendTask.waitUntilFinished(2000);
        } catch (InterruptedException ex) {
        }
    }

}
