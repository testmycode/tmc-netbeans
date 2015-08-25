package fi.helsinki.cs.tmc.events;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
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
    private Queue<TmcEvent> eventQueue;

    private static interface Wrapper {
        public boolean wraps(TmcEventListener that);
    }

    private static class WeakListener extends TmcEventListener implements Wrapper {
        private WeakReference<TmcEventListener> weakRef;

        public WeakListener(TmcEventListener listener) {
            this.weakRef = new WeakReference<TmcEventListener>(listener);
        }

        @Override
        public void receive(TmcEvent event) throws Throwable {
            TmcEventListener listener = weakRef.get();
            if (listener != null) {
                listener.receive(event);
            }
        }

        @Override
        public boolean mayBeUnsubscribed() {
            return weakRef.get() == null;
        }

        @Override
        public boolean wraps(TmcEventListener that) {
            return that == weakRef.get();
        }
    }

    private static class DependentListener extends TmcEventListener implements Wrapper {
        private TmcEventListener listener;
        private WeakReference<Object> weakRef;

        public DependentListener(TmcEventListener listener, Object dependency) {
            this.listener = listener;
            this.weakRef = new WeakReference<Object>(dependency);
        }

        @Override
        public void receive(TmcEvent event) throws Throwable {
            listener.receive(event);
        }

        @Override
        public boolean mayBeUnsubscribed() {
            return weakRef.get() == null;
        }

        @Override
        public boolean wraps(TmcEventListener that) {
            return listener == that;
        }
    }

    private TmcEventBus() {
        this.listeners = new ArrayList<TmcEventListener>();
        this.eventQueue = new ArrayDeque<TmcEvent>();
    }

    /**
     * Subscribes a listener that is never unsubscribed automatically.
     */
    public synchronized void subscribeStrongly(TmcEventListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Subscribes a weak reference to a listener.
     * After all normal references to the listener disappear, it will eventually be unsubscribed.
     */
    public synchronized void subscribeWeakly(TmcEventListener listener) {
        this.listeners.add(new WeakListener(listener));
    }

    /**
     * Subscribes a listener that is eventually removed after a given dependency is garbage-collected.
     */
    public synchronized void subscribeDependent(TmcEventListener listener, Object dependency) {
        this.listeners.add(new DependentListener(listener, dependency));
    }

    public synchronized void unsubscribe(TmcEventListener toRemove) {
        Iterator<TmcEventListener> iter = listeners.iterator();
        while (iter.hasNext()) {
            TmcEventListener listener = iter.next();
            boolean remove =
                    (listener == toRemove)
                            || (listener instanceof Wrapper
                                    && ((Wrapper) listener).wraps(toRemove));
            if (remove) {
                iter.remove();
                break;
            }
        }
    }

    public synchronized void post(TmcEvent event) {
        eventQueue.add(event);
        processEventQueue();
    }

    private void processEventQueue() {
        // This handles post() during post()
        // but not yet subscribe*() during post().
        while (!eventQueue.isEmpty()) {
            TmcEvent event = eventQueue.remove();
            Iterator<TmcEventListener> iter = listeners.iterator();
            while (iter.hasNext()) {
                TmcEventListener listener = iter.next();
                if (listener.mayBeUnsubscribed()) {
                    iter.remove();
                } else {
                    try {
                        listener.receive(event);
                    } catch (Throwable ex) {
                        log.log(Level.WARNING, ex.getMessage(), ex);
                    }
                }
            }
        }
    }
}
