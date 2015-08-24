package fi.helsinki.cs.tmc.spyware;

import fi.helsinki.cs.tmc.utilities.LazyHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

/**
 * Buffers the latest version of an event and sends it at most with a certain interval.
 *
 * <p>An event rate limiter forwards events to another receiver.
 * When an event with a key {@code K} comes in,
 * a cooldown is activated. No other event with key {@code K} will be sent while
 * the cooldown is active. When the cooldown for {@code K} expires,
 * the most recent {@code K} received during the cooldown period is sent,
 * if any.
 *
 * <p>Separating the concern of rate limiting here allows event sources to
 * fire as many events as they want, as long as they are fine with some events
 * being discarded.
 */
@Deprecated // Decided against using it for now. Might will use later, so won't delete yet (20120513).
public class EventRateLimiter implements EventReceiver {
    public static final long DEFAULT_COOLDOWN = 30*1000;

    private class EventKeyRecord {
        private long cooldownLength = DEFAULT_COOLDOWN;
        private LoggableEvent newestUnsent = null;
        private Timer cooldownTimer = null;

        public synchronized void receive(LoggableEvent ev) {
            if (cooldownTimer == null) {
                assert newestUnsent == null;
                nextReceiver.receiveEvent(ev);

                cooldownTimer = new Timer("EventRateLimiter cooldown", true);
                cooldownTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        forwardFromTimer();
                    }
                }, cooldownLength);
            } else {
                newestUnsent = ev;
            }
        }

        public synchronized void setCooldown(long cooldownLength) {
            this.cooldownLength = cooldownLength;
        }

        public synchronized void close() { //TODO: unit test
            if (cooldownTimer != null) {
                cooldownTimer.cancel();
            }
            if (newestUnsent != null) {
                nextReceiver.receiveEvent(newestUnsent);
                newestUnsent = null;
            }
        }

        private synchronized void forwardFromTimer() {
            if (newestUnsent != null) {
                nextReceiver.receiveEvent(newestUnsent);
                newestUnsent = null;
            }

            cooldownTimer = null;
        }
    }

    private final EventReceiver nextReceiver;

    private final LazyHashMap<String, EventKeyRecord> recordsByKey = new LazyHashMap<String, EventKeyRecord>(new Callable<EventKeyRecord>() {
        @Override
        public EventKeyRecord call() throws Exception {
            return new EventKeyRecord();
        }
    });

    public EventRateLimiter(EventReceiver nextReceiver) {
        this.nextReceiver = nextReceiver;
    }

    public synchronized void setCooldownForEventKey(String eventKey, long delayMillis) {
        recordsByKey.get(eventKey).setCooldown(delayMillis);
    }

    @Override
    public synchronized void receiveEvent(LoggableEvent event) {
        recordsByKey.get(event.getKey()).receive(event);
    }

    /**
     * Flush and close.
     */
    @Override
    public void close() {
        for (EventKeyRecord rec : recordsByKey.values()) {
            rec.close();
        }
        recordsByKey.clear();
    }
}