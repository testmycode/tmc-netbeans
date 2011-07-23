package fi.helsinki.cs.tmc.utilities;

import java.util.concurrent.Future;
import fi.helsinki.cs.tmc.testing.MockBgTaskListener;
import java.util.concurrent.Callable;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BgTaskTest {
    
    private MockBgTaskListener<String> listener;
    
    @Before
    public void setUp() {
        listener = new MockBgTaskListener<String>();
    }
    
    @Test
    public void whenTheTaskCompletesItShouldCallTheListenerInTheSwingThread() {
        Callable<String> callable = new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "yay!";
            }
        };
        
        new BgTask("My succeeding task", listener, callable).start();
        
        listener.waitForCall();
        assertEquals("yay!", listener.result);
    }
    
    @Test
    public void whenTheTaskFailsItShouldCallTheListenerInTheSwingThread() {
        final Exception ex = new Exception();
        Callable<String> callable = new Callable<String>() {
            @Override
            public String call() throws Exception {
                throw ex;
            }
        };
        
        new BgTask("My failing task", listener, callable).start();
        
        listener.waitForCall();
        assertSame(ex, listener.taskException);
    }
    
    @Test
    public void whenTheTaskIsCancelledItShouldCallTheListenerInTheSwingThread() {
        final Exception ex = new Exception();
        Callable<String> callable = new Callable<String>() {
            @Override
            public String call() throws InterruptedException {
                Thread.sleep(10000);
                return "should not get here";
            }
        };
        
        Future<?> future = new BgTask("My failing task", listener, callable).start();
        future.cancel(true);
        
        listener.waitForCall();
        assertTrue(listener.cancelled);
    }
}
