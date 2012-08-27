package fi.helsinki.cs.tmc.utilities;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class DelayedRunnerTest {
    
    private DelayedRunner runner;
    
    private Semaphore counter;
    
    private Runnable task = new Runnable() {
        @Override
        public void run() {
            counter.release();
        }
    };
    
    @Before
    public void setUp() {
        runner = new DelayedRunner();
        counter = new Semaphore(0);
    }
    
    @Test
    public void canBeRunMultipleTimes() throws InterruptedException {
        runner.setDelay(5);
        
        runner.setTask(task);
        counter.acquire();
        runner.setTask(task);
        counter.acquire();
        assertFalse(counter.tryAcquire(200, TimeUnit.MILLISECONDS));
    }
    
    @Test
    public void taskCanBeReplacedBeforeItFires() throws InterruptedException {
        runner.setDelay(300);
        
        runner.setTask(task);
        runner.setTask(task);
        counter.acquire();
        assertFalse(counter.tryAcquire(500, TimeUnit.MILLISECONDS));
    }
}
