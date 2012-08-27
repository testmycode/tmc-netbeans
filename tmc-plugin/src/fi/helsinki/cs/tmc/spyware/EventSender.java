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
    
    private static EventSender instance;

    public static EventSender getInstance() {
        return instance;
    }
    
    public static long DEFAULT_DELAY = 5*60*1000;
    public static int DEFAULT_MAX_EVENTS = 4096;
    
    private long delay = DEFAULT_DELAY;
    private int maxEvents = DEFAULT_MAX_EVENTS;
    
    private ArrayList<LoggableEvent> buffer;
    private java.util.Timer sendTimer;
    
    public EventSender() {
        this.buffer = new ArrayList<LoggableEvent>();
        this.sendTimer = new java.util.Timer("EventSender timer", true);
        this.sendTimer.schedule(sendTask, delay, delay);
    }
    
    public synchronized void sendNow() {
        sendTask.run();
    }
    
    @Override
    public synchronized void receiveEvent(LoggableEvent event) {
        buffer.add(event);
        removeIfOverLimit();
    }
    
    public synchronized ArrayList<LoggableEvent> takeBuffer() {
        ArrayList<LoggableEvent> oldBuf = buffer;
        buffer = new ArrayList<LoggableEvent>();
        return oldBuf;
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
        private final Object doneCondVar = new Object();
        private volatile boolean running = false;
        
        // run() is synchronized because it may be called by the timer as well as sendNow().
        @Override
        public synchronized void run() {
            synchronized (doneCondVar) {
                running = true;
            }
            
            try {
                doSend();
            } finally {
                synchronized (doneCondVar) {
                    running = false;
                    doneCondVar.notifyAll();
                }
            }
        }
        
        private void doSend() {
            final List<LoggableEvent> events = takeBuffer();
            if (events.isEmpty()) {
                return;
            }
            
            log.log(Level.INFO, "Sending {0} events", events.size());
            
            ServerAccess serverAccess = new ServerAccess();
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
