package fi.helsinki.cs.tmc.testing;

import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import static org.junit.Assert.*;

public class MockBgTaskListener<T> implements BgTaskListener<T> {
    public boolean success = false;
    public T result = null;
    public boolean cancelled = false;
    public Throwable taskException = null;
    
    private String testFailure = null;
    private Semaphore semaphore = new Semaphore(0);

    @Override
    public void backgroundTaskReady(T result) {
        checkStatus();
        success = true;
        this.result = result;
        semaphore.release();
    }

    @Override
    public void backgroundTaskCancelled() {
        checkStatus();
        this.cancelled = true;
        semaphore.release();
    }

    @Override
    public void backgroundTaskFailed(Throwable ex) {
        checkStatus();
        this.taskException = ex;
        semaphore.release();
    }

    private void checkStatus() {
        if (!SwingUtilities.isEventDispatchThread()) {
            testFailure = "BgTaskListener called from the wrong thread";
        }
    }
    
    /**
     * Waits until a background process calls this.
     * 
     * Fails if the call was not made from the Swing EDT.
     */
    public void waitForCall() {
        try {
            if (!semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                fail("Background task listener was not called.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (testFailure != null) {
            fail(testFailure);
        }
    }

    public void assertGotSuccess() {
        if (taskException != null) {
            fail("Expected success but got failure:\n" + ExceptionUtils.backtraceToString(taskException));
        } else if (cancelled) {
            fail("Expected success but was cancelled");
        } else if (!success) {
            fail("Expected success but no call received. Did you forget waitForCall()?");
        }
    }
    
}
