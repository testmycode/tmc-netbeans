package fi.helsinki.cs.tmc.runners;

import fi.helsinki.cs.tmc.data.TestRunResult;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import java.util.concurrent.Callable;

/**
 * ExerciseRunner for implementing runner tasks
 */
public interface ExerciseRunner {
    public abstract Callable<TestRunResult> getTestRunningTask(TmcProjectInfo projectInfo);
}
