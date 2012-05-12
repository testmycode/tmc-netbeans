package fi.helsinki.cs.tmc.spyware;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Vector;
import org.junit.Before;
import static org.junit.Assert.*;

public abstract class EventForwardedTestBase {
    protected ArrayList<LoggableEvent> eventsSent;
    protected Vector<LoggableEvent> eventsReceived;
    
    protected final EventReceiver receiver = new EventReceiver() {
        @Override
        public void receiveEvent(LoggableEvent event) {
            eventsReceived.add(event);
        }

        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };
    
    protected abstract EventReceiver getSystemUnderTest();

    @Before
    public void setUpBase() {
        eventsSent = new ArrayList<LoggableEvent>();
        eventsReceived = new Vector<LoggableEvent>();
    }
    
    protected void sendEvent(LoggableEvent event) {
        getSystemUnderTest().receiveEvent(event);
        eventsSent.add(event);
    }
    
    protected void sendEvents(int count, String type) {
        for (int i = 0; i < count; ++i) {
            LoggableEvent event = new LoggableEvent("course", "exercise", type, ("event" + eventsSent.size()).getBytes(Charset.forName("UTF-8"))) {
                @Override
                public String toString() {
                    return this.getEventType() + "_" + new String(this.getData(), Charset.forName("UTF-8"));
                }
            };
            sendEvent(event);
        }
    }
    
    protected void assertReceivedExactly(int... expectedSentMsgIndices) {
        int[] receivedIndices = new int[eventsReceived.size()];
        int i = 0;
        for (LoggableEvent ev : eventsReceived) {
            receivedIndices[i++] = eventsSent.indexOf(ev);
        }
        
        assertArrayEquals("Wrong messages forwarded.", expectedSentMsgIndices, receivedIndices);
    }

}
