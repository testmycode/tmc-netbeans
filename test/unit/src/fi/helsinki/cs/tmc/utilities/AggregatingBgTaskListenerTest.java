package fi.helsinki.cs.tmc.utilities;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AggregatingBgTaskListenerTest {

    @Mock private BgTaskListener<Collection<String>> aggregateListener;
    @Captor private ArgumentCaptor<Collection<String>> resultCaptor;
    
    private AggregatingBgTaskListener<String> aggregator;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    
    private void createAggregator(int expectedCount) {
        aggregator = new AggregatingBgTaskListener<String>(expectedCount, aggregateListener);
    }
    
    @Test
    public void whenEnoughSubtasksHaveFinishedItShouldSignalSuccess() {
        createAggregator(3);
        aggregator.bgTaskReady("one");
        aggregator.bgTaskReady("two");
        verifyZeroInteractions(aggregateListener);
        aggregator.bgTaskReady("three");
        verify(aggregateListener).bgTaskReady(resultCaptor.capture());
    }
    
    @Test
    public void whenASubtaskFailsItShouldSignalFailure() {
        createAggregator(2);
        aggregator.bgTaskReady("one");
        Exception ex = new Exception("oops");
        aggregator.bgTaskFailed(ex);
        verify(aggregateListener).bgTaskFailed(ex);
    }
    
    @Test
    public void whenASubtaskFailsMultipleTimesItShouldSignalOnlyOnce() {
        createAggregator(2);
        Exception ex1 = new Exception("oops one");
        Exception ex2 = new Exception("oops two");
        aggregator.bgTaskFailed(ex1);
        aggregator.bgTaskFailed(ex2);
        verify(aggregateListener, only()).bgTaskFailed(ex1);
    }
    
    @Test
    public void whenASubtaskFailsItShouldNotSignalFutureSuccesses() {
        createAggregator(3);
        aggregator.bgTaskReady("one");
        Exception ex = new Exception("oops");
        aggregator.bgTaskFailed(ex);
        aggregator.bgTaskReady("two");
        aggregator.bgTaskReady("three");
        verify(aggregateListener, only()).bgTaskFailed(ex);
    }
    
    @Test
    public void whenASubtaskIsCanceledItShouldSignalCancellation() {
        createAggregator(2);
        aggregator.bgTaskReady("one");
        aggregator.bgTaskCancelled();
        verify(aggregateListener).bgTaskCancelled();
    }
    
    @Test
    public void whenMultipleSubtasksAreCancelledShouldSignalCancellationOnlyOnce() {
        createAggregator(2);
        aggregator.bgTaskCancelled();
        aggregator.bgTaskCancelled();
        verify(aggregateListener, only()).bgTaskCancelled();
    }
    
    @Test
    public void whenASubtaskIsCancelledItShouldNotSignalFutureSuccesses() {
        createAggregator(3);
        aggregator.bgTaskReady("one");
        aggregator.bgTaskCancelled();
        aggregator.bgTaskReady("two");
        aggregator.bgTaskReady("three");
        verify(aggregateListener, only()).bgTaskCancelled();
    }
    
    @Test(expected=IllegalStateException.class)
    public void whenTooSubtasksSucceedItShouldThrowExceptions() {
        createAggregator(3);
        aggregator.bgTaskReady("one");
        aggregator.bgTaskReady("two");
        aggregator.bgTaskReady("three");
        aggregator.bgTaskReady("four");
    }
    
    
}
