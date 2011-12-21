package fi.helsinki.cs.tmc.utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Calls a list of listeners in sequence.
 *
 * <p>
 * If a listener throws an exception, subsequent listeners will not be called.
 */
public class BgTaskListenerList<T> implements BgTaskListener<T> {

    private List<BgTaskListener<T>> listeners;

    public BgTaskListenerList() {
        this.listeners = new ArrayList<BgTaskListener<T>>();
    }

    public void addListener(BgTaskListener<T> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void bgTaskReady(T result) {
        for (BgTaskListener listener : listeners) {
            listener.bgTaskReady(result);
        }
    }

    @Override
    public void bgTaskCancelled() {
        for (BgTaskListener listener : listeners) {
            listener.bgTaskCancelled();
        }
    }

    @Override
    public void bgTaskFailed(Throwable ex) {
        for (BgTaskListener listener : listeners) {
            listener.bgTaskFailed(ex);
        }
    }

}
