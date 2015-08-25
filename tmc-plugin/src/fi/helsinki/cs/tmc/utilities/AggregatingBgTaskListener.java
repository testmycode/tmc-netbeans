package fi.helsinki.cs.tmc.utilities;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Waits for multiple BG tasks to finish and fails (or is canceled) if
 * any of them fails (or is canceled).
 */
public class AggregatingBgTaskListener<T> implements BgTaskListener<T> {

    private final int expectedCount;
    private final BgTaskListener<Collection<T>> aggregateListener;
    private ArrayList<T> received;
    private boolean complete = false;

    public AggregatingBgTaskListener(
            int expectedCount, BgTaskListener<Collection<T>> aggregateListener) {
        this.expectedCount = expectedCount;
        this.aggregateListener = aggregateListener;
        this.received = new ArrayList<T>(expectedCount);
    }

    @Override
    public void bgTaskReady(T result) {
        if (!complete) {
            if (received.size() == expectedCount) {
                throw new IllegalStateException(
                        this.getClass().getSimpleName() + " expected only " + expectedCount);
            }
            received.add(result);
            if (received.size() == expectedCount) {
                aggregateListener.bgTaskReady(received);
            }
        }
    }

    @Override
    public void bgTaskFailed(Throwable ex) {
        if (!complete) {
            complete = true;
            aggregateListener.bgTaskFailed(ex);
        }
    }

    @Override
    public void bgTaskCancelled() {
        if (!complete) {
            complete = true;
            aggregateListener.bgTaskCancelled();
        }
    }
}
