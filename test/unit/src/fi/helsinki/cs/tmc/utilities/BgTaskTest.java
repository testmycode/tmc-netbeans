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
        
        BgTask.start("My succeeding task", listener, callable);
        
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
        
        BgTask.start("My failing task", listener, callable);
        
        listener.waitForCall();
        assertSame(ex, listener.taskException);
    }
    
    @Test
    public void whenTheTaskIsCancelledItShouldCallTheListenerInTheSwingThread() {
        Callable<String> callable = new Callable<String>() {
            @Override
            public String call() throws InterruptedException {
                Thread.sleep(10000);
                return "should not get here";
            }
        };
        
        Future<?> future = BgTask.start("My failing task", listener, callable);
        future.cancel(true);
        
        listener.waitForCall();
        assertTrue(listener.cancelled);
    }
}
