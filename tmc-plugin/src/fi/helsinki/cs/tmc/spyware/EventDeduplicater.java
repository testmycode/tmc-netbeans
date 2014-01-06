package fi.helsinki.cs.tmc.spyware;

import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Forwards events to another receiver but discards consecutive events with the same key and data.
 * Only applied to certain event sources.
 */
public class EventDeduplicater implements EventReceiver {
    private EventReceiver nextReceiver;

    private Map<String, byte[]> lastHashByKey = new HashMap<String, byte[]>();

    public EventDeduplicater(EventReceiver nextReceiver) {
        this.nextReceiver = nextReceiver;
    }

    @Override
    public synchronized void receiveEvent(LoggableEvent event) {
        byte[] prevHash = lastHashByKey.get(event.getKey());
        byte[] newHash = hash(event.getData());
        boolean changed = (prevHash == null || !Arrays.equals(prevHash, newHash));
        if (changed) {
            nextReceiver.receiveEvent(event);
            lastHashByKey.put(event.getKey(), newHash);
        }
    }

    private byte[] hash(byte[] data) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw ExceptionUtils.toRuntimeException(ex);
        }
        return md.digest(data);
    }

    @Override
    public void close() {
    }
    
}
