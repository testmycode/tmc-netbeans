package fi.helsinki.cs.tmc.spyware;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.NbTmcSettings;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.utilities.Cooldown;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import fi.helsinki.cs.tmc.utilities.SingletonTask;
import fi.helsinki.cs.tmc.utilities.TmcRequestProcessor;
import fi.helsinki.cs.tmc.core.communication.HttpResult;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;

/**
 * Buffers {@link LoggableEvent}s and sends them to the server and/or syncs them
 * to the disk periodically.
 */
public class EventSendBuffer implements EventReceiver {

    private static final Logger log = Logger.getLogger(EventSendBuffer.class.getName());

    public static final long DEFAULT_SEND_INTERVAL = 3 * 60 * 1000;
    public static final long DEFAULT_SAVE_INTERVAL = 3 * 60 * 1000;
    public static final int DEFAULT_MAX_EVENTS = 64 * 1024;
    public static final int DEFAULT_AUTOSEND_THREHSOLD = DEFAULT_MAX_EVENTS / 2;
    public static final int DEFAULT_AUTOSEND_COOLDOWN = 30 * 1000;

    private Random random = new Random();
    private SpywareSettings settings;
    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private EventStore eventStore;

    // The following variables must only be accessed with a lock on sendQueue.
    private final ArrayDeque<LoggableEvent> sendQueue = new ArrayDeque<LoggableEvent>();
    private int eventsToRemoveAfterSend = 0;
    private int maxEvents = DEFAULT_MAX_EVENTS;
    private int autosendThreshold = DEFAULT_AUTOSEND_THREHSOLD;
    private Cooldown autosendCooldown;

    public EventSendBuffer(SpywareSettings settings, ServerAccess serverAccess, CourseDb courseDb, EventStore eventStore) {
        this.settings = settings;
        this.serverAccess = serverAccess;
        this.courseDb = courseDb;
        this.eventStore = eventStore;
        this.autosendCooldown = new Cooldown(DEFAULT_AUTOSEND_COOLDOWN);

        try {
            List<LoggableEvent> initialEvents = Arrays.asList(eventStore.load());
            initialEvents = initialEvents.subList(0, Math.min(maxEvents, initialEvents.size()));
            this.sendQueue.addAll(initialEvents);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Failed to read events from event store", ex);
        } catch (RuntimeException ex) {
            log.log(Level.WARNING, "Failed to read events from event store", ex);
        }

        this.sendingTask.setInterval(DEFAULT_SEND_INTERVAL);
        this.savingTask.setInterval(DEFAULT_SAVE_INTERVAL);
    }

    public void setSendingInterval(long interval) {
        this.sendingTask.setInterval(interval);
    }

    public void setSavingInterval(long interval) {
        this.savingTask.setInterval(interval);
    }

    public void setMaxEvents(int newMaxEvents) {
        if (newMaxEvents <= 0) {
            throw new IllegalArgumentException();
        }

        synchronized (sendQueue) {
            if (newMaxEvents < maxEvents) {
                int diff = newMaxEvents - maxEvents;
                for (int i = 0; i < diff; ++i) {
                    sendQueue.pop();
                }
                eventsToRemoveAfterSend -= diff;
            }

            maxEvents = newMaxEvents;
        }
    }

    public void setAutosendThreshold(int autosendThreshold) {
        synchronized (sendQueue) {
            if (autosendThreshold <= 0) {
                throw new IllegalArgumentException();
            }
            this.autosendThreshold = autosendThreshold;

            maybeAutosend();
        }
    }

    public void setAutosendCooldown(long durationMillis) {
        this.autosendCooldown.setDurationMillis(durationMillis);
    }

    public void sendNow() {
        sendingTask.start();
    }

    public void saveNow(long timeout) throws TimeoutException, InterruptedException {
        savingTask.start();
        savingTask.waitUntilFinished(timeout);
    }

    public void waitUntilCurrentSendingFinished(long timeout) throws TimeoutException, InterruptedException {
        sendingTask.waitUntilFinished(timeout);
    }

    @Override
    public void receiveEvent(LoggableEvent event) {
        if (!settings.isSpywareEnabled()) {
            return;
        }

        synchronized (sendQueue) {
            if (sendQueue.size() >= maxEvents) {
                sendQueue.pop();
                eventsToRemoveAfterSend--;
            }
            sendQueue.add(event);

            maybeAutosend();
        }
    }

    private void maybeAutosend() {
        if (sendQueue.size() >= autosendThreshold && autosendCooldown.isExpired()) {
            autosendCooldown.start();
            sendNow();
        }
    }

    /**
     * Stops sending any more events.
     *
     * Buffer manipulation methods may still be called.
     */
    @Override
    public void close() {
        long delayPerWait = 2000;
        try {
            sendingTask.unsetInterval();
            savingTask.unsetInterval();

            savingTask.waitUntilFinished(delayPerWait);
            savingTask.start();
            savingTask.waitUntilFinished(delayPerWait);

            sendingTask.waitUntilFinished(delayPerWait);

        } catch (TimeoutException ex) {
            log.log(Level.WARNING, "Time out when closing EventSendBuffer", ex);
        } catch (InterruptedException ex) {
            log.log(Level.WARNING, "Closing EventSendBuffer interrupted", ex);
        }
    }

    private SingletonTask sendingTask = new SingletonTask(new Runnable() {
        // Sending too many at once may go over the server's POST size limit.
        private static final int MAX_EVENTS_PER_SEND = 500;

        @Override
        public void run() {
            boolean shouldSendMore;

            do {
                ArrayList<LoggableEvent> eventsToSend = copyEventsToSendFromQueue();
                if (eventsToSend.isEmpty()) {
                    return;
                }

                synchronized (sendQueue) {
                    shouldSendMore = sendQueue.size() > eventsToSend.size();
                }

                String url = pickDestinationUrl();
                if (url == null) {
                    return;
                }

                log.log(Level.INFO, "Sending {0} events to {1}", new Object[]{eventsToSend.size(), url});

                doSend(eventsToSend, url);
            } while (shouldSendMore);
        }

        private ArrayList<LoggableEvent> copyEventsToSendFromQueue() {
            synchronized (sendQueue) {
                ArrayList<LoggableEvent> eventsToSend = new ArrayList<LoggableEvent>(sendQueue.size());

                Iterator<LoggableEvent> i = sendQueue.iterator();
                while (i.hasNext() && eventsToSend.size() < MAX_EVENTS_PER_SEND) {
                    eventsToSend.add(i.next());
                }

                eventsToRemoveAfterSend = eventsToSend.size();

                return eventsToSend;
            }
        }

        private String pickDestinationUrl() {
            Course course = courseDb.getCurrentCourse();
            if (course == null) {
                log.log(Level.FINE, "Not sending events because no course selected");
                return null;
            }

            List<String> urls = course.getSpywareUrls();
            if (urls == null || urls.isEmpty()) {
                log.log(Level.INFO, "Not sending events because no URL provided by server");
                return null;
            }

            String url = urls.get(random.nextInt(urls.size()));

            return url;
        }

        /**
         * Converts events to data[] and sends it to defined url.
         */
        private void doSend(final ArrayList<LoggableEvent> eventsToSend, final String url) {       
            NbTmcSettings settings = NbTmcSettings.getDefault();
            Optional<Course> currentCourse = settings.getCurrentCourse();
            if (!currentCourse.isPresent()) {
                return;
            }
            addCorrectSpywareUrl(url, currentCourse);
            final ProgressHandle progress = ProgressHandleFactory.createSystemHandle("Sending stats");
            progress.start();
            try {
                byte[] data = convertEventsToByteArray(eventsToSend);
                ListenableFuture<List<HttpResult>> spywareSending = 
                        TmcCoreSingleton.getInstance().sendSpywareDiffs(data);
                Futures.addCallback(spywareSending, new FutureCallback<List<HttpResult>>() {
                    @Override
                    public void onSuccess(List<HttpResult> success) {
                        clearAfterSend(success);
                    }
                    @Override
                    public void onFailure(Throwable thrwbl) {
                        clearAfterSend(new ArrayList<HttpResult>());
                        System.err.println(thrwbl.getMessage());
                    }
                    private void clearAfterSend(List<HttpResult> success) {
                        
                        progress.finish();
                        log.log(Level.INFO, "Sent {0} events successfully to {1}", new Object[]{eventsToSend.size(), url});
                        removeSentEventsFromQueue();
                        // If saving fails now (or is already running and fails later)
                        // then we may end up sending duplicate events later.
                        // This will hopefully be very rare.
                        savingTask.start();
                    }
                });

            } catch (TmcCoreException ex) {
                progress.finish();
                Exceptions.printStackTrace(ex);
            }
        }

        private void addCorrectSpywareUrl(final String url, Optional<Course> currentCourse) {
            List<String> spywareUrls = new ArrayList<String>();
            String finalUrl = serverAccess.addApiCallQueryParameters(url);
            spywareUrls.add(finalUrl);
            currentCourse.get().setSpywareUrls(spywareUrls);
        }

        private byte[] convertEventsToByteArray(final ArrayList<LoggableEvent> eventsToSend) throws RuntimeException {
            byte[] data;
            try {
                data = serverAccess.eventListToPostBody(eventsToSend);
            } catch (IOException ex) {
                throw ExceptionUtils.toRuntimeException(ex);
            }
            return data;
        }
        
        private void removeSentEventsFromQueue() {
            synchronized (sendQueue) {
                assert (eventsToRemoveAfterSend <= sendQueue.size());
                while (eventsToRemoveAfterSend > 0) {
                    sendQueue.pop();
                    eventsToRemoveAfterSend--;
                }
            }
        }

    }, TmcRequestProcessor.instance);

    private SingletonTask savingTask = new SingletonTask(new Runnable() {
        @Override
        public void run() {
            try {
                LoggableEvent[] eventsToSave;
                synchronized (sendQueue) {
                    eventsToSave = Iterables.toArray(sendQueue, LoggableEvent.class);
                }
                eventStore.save(eventsToSave);
            } catch (IOException ex) {
                log.log(Level.WARNING, "Failed to save events", ex);
            }
        }
    }, TmcRequestProcessor.instance);
}
