package fi.helsinki.cs.tmc.spyware;

import org.junit.Before;
import org.junit.Test;

public class EventRateLimiterTest extends EventForwardedTestBase {
    private final long reasonableTime = 300;
    
    private EventRateLimiter limiter;
    
    @Before
    public void setUp() {
        limiter = new EventRateLimiter(receiver);
    }

    @Override
    protected EventReceiver getSystemUnderTest() {
        return limiter;
    }
    
    @Test
    public void testDeliversOnlyLatestEventWhenSpammed() {
        limiter.setCooldownForEventKey("course|exercise|one", reasonableTime);
        limiter.setCooldownForEventKey("course|exercise|two", reasonableTime * 2);
        
        sendEvents(30, "one");
        sendEvents(20, "two");
        sleep(reasonableTime * 1.1);
        sendEvents(20, "two");
        sleep(reasonableTime * 2.1);
        
        assertReceivedExactly(0, 30, 29, 30+20+19);
    }
    
    @Test
    public void testWorksWithSparseEvents() {
        limiter.setCooldownForEventKey("course|exercise|one", reasonableTime);
        
        sendEvents(2, "one");
        sleep(reasonableTime * 1.1);
        sendEvents(1, "one");
        sleep(reasonableTime * 1.1);
        sendEvents(1, "one");
        sleep(reasonableTime * 1.1);
        sendEvents(1, "one");
        sleep(reasonableTime * 1.1);
        
        assertReceivedExactly(0, 1, 2, 3, 4);
    }
    
    @Test
    public void testFlushesOnClose() {
        limiter.setCooldownForEventKey("course|exercise|one", reasonableTime);
        
        sendEvents(3, "one");
        sleep(reasonableTime * 1.1);
        sendEvents(3, "one");
        
        limiter.close();
        
        assertReceivedExactly(0, 2, 3, 5);
        
        sleep(reasonableTime * 1.1);
        
        assertReceivedExactly(0, 2, 3, 5);
    }
    
    @Test
    public void testCloseWithNothingPending() {
        limiter.setCooldownForEventKey("course|exercise|one", reasonableTime);
        
        sendEvents(3, "one");
        sleep(reasonableTime * 1.1);
        
        limiter.close();
        
        assertReceivedExactly(0, 2);
        
        sleep(reasonableTime * 1.1);
        
        assertReceivedExactly(0, 2);
    }
    
    private void sleep(double time) {
        try {
            Thread.sleep((long)time);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
