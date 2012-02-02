package fi.helsinki.cs.tmc.events;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TmcEventBus {
    private static final Logger log = Logger.getLogger(TmcEventBus.class.getName());
    private static final TmcEventBus instance = new TmcEventBus();

    public static TmcEventBus getDefault() {
        return instance;
    }
    
    // Factory method to avoid accidental creation when getDefault was meant.
    public static TmcEventBus createNewInstance() {
        return new TmcEventBus();
    }

    private List<TmcEventListener> listeners;

    private TmcEventBus() {
        this.listeners = new ArrayList<TmcEventListener>();
    }

    public void subscribe(TmcEventListener listener) {
        this.listeners.add(listener);
    }

    public void unsubscribe(TmcEventListener listener) {
        this.listeners.remove(listener);
    }

    public void post(TmcEvent event) {
        for (TmcEventListener listener : this.listeners) {
            try {
                listener.receive(event);
            } catch (Throwable ex) {
                log.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
    }
}
