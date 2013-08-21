package fi.helsinki.cs.tmc.spyware;

import java.nio.charset.Charset;
import org.junit.Before;
import org.junit.Test;

public class EventDeduplicaterTest extends EventForwardedTestBase {
    private EventDeduplicater dedup;
    
    @Before
    public void setUp() {
        dedup = new EventDeduplicater(receiver);
    }
    
    @Override
    protected EventReceiver getSystemUnderTest() {
        return dedup;
    }
    
    private LoggableEvent mkEvent(String exerciseName, String type, String data) {
        return new LoggableEvent("course1", exerciseName, type, data.getBytes(Charset.forName("UTF-8")));
    }
    
    @Test
    public void testPassesThroughNonDuplicates() {
        LoggableEvent ev0 = mkEvent("ex1", "ty1", "data1");
        LoggableEvent ev1 = mkEvent("ex1", "ty1", "data2");
        LoggableEvent ev2 = mkEvent("ex1", "ty2", "data1");
        LoggableEvent ev3 = mkEvent("ex2", "ty1", "data1");
        
        sendEvent(ev0);
        sendEvent(ev1);
        sendEvent(ev2);
        sendEvent(ev3);
        
        assertReceivedExactly(0, 1, 2, 3);
    }
    
    @Test
    public void testDiscardsConsecutiveDuplicates() {
        LoggableEvent ev0 = mkEvent("ex1", "ty1", "data1");
        LoggableEvent ev1 = mkEvent("ex1", "ty1", "data1");
        LoggableEvent ev2 = mkEvent("ex1", "ty1", "data1");
        LoggableEvent ev3 = mkEvent("ex2", "ty1", "data1");
        LoggableEvent ev4 = mkEvent("ex2", "ty1", "data1");
        
        sendEvent(ev0);
        sendEvent(ev1);
        sendEvent(ev2);
        sendEvent(ev3);
        sendEvent(ev4);
        
        assertReceivedExactly(0, 3);
    }
    
    @Test
    public void testDoesNotDiscardNonConsecutiveDuplicates() {
        LoggableEvent ev1 = mkEvent("ex1", "ty1", "data1");
        LoggableEvent ev2 = mkEvent("ex1", "ty1", "data2");
        LoggableEvent ev3 = mkEvent("ex1", "ty1", "data1");
        
        sendEvent(ev1);
        sendEvent(ev2);
        sendEvent(ev3);
        
        assertReceivedExactly(0, 1, 2);
    }
}
