package fi.helsinki.cs.tmc.runners;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.data.TestRunResult;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import java.util.concurrent.Callable;

/**
 * ExerciseRunner for implementing runner tasks
 * Using Optional as a container for the result itself, if TestRunResult is not present
 * compilation failure should be assumed.
 *  
 */
public interface ExerciseRunner {
    public abstract Callable<Optional<TestRunResult>> getTestRunningTask(TmcProjectInfo projectInfo);
}
