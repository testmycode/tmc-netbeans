package fi.helsinki.cs.tmc.spyware;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Forwards events to another receiver but discards consecutive duplicates with the same key.
 */
public class EventDeduplicater implements EventReceiver {
    private EventReceiver nextReceiver;
    
    private Map<String, LoggableEvent> lastByKey = new HashMap<String, LoggableEvent>();

    public EventDeduplicater(EventReceiver nextReceiver) {
        this.nextReceiver = nextReceiver;
    }

    @Override
    public synchronized void receiveEvent(LoggableEvent event) {
        LoggableEvent previous = lastByKey.get(event.getKey());
        if (previous == null || !Arrays.equals(previous.getData(), event.getData())) {
            nextReceiver.receiveEvent(event);
        }
        lastByKey.put(event.getKey(), event);
    }

    @Override
    public void close() {
    }
    
}
