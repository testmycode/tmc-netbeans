package fi.helsinki.cs.tmc.events;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TmcEventBus {
    private static final Logger log = Logger.getLogger(TmcEventBus.class.getName());
    private static final TmcEventBus instance = new TmcEventBus();

    public static TmcEventBus getInstance() {
        return instance;
    }

    private List<TmcEventListener> listeners;

    public TmcEventBus() {
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
