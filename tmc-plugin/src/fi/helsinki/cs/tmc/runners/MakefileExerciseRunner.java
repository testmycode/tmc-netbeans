package fi.helsinki.cs.tmc.runners;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.TestRunResult;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.UserVisibleException;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.process.ProcessResult;
import fi.helsinki.cs.tmc.utilities.process.ProcessRunner;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public class MakefileExerciseRunner extends AbstractExerciseRunner {

    private static final Logger log = Logger.getLogger(MakefileExerciseRunner.class.getName());

    @Override
    public Callable<Optional<TestRunResult>> getTestRunningTask(final TmcProjectInfo projectInfo) {
        final InputOutput io = IOProvider.getDefault().getIO(projectInfo.getProjectName(), false);

        return new Callable<Optional<TestRunResult>>() {
            @Override
            public Optional<TestRunResult> call() throws Exception {
                log.log(Level.INFO, "Compiling project {0}", projectInfo.getProjectName());
                Project project = projectInfo.getProject();
                FileObject makeFile = project.getProjectDirectory().getFileObject("Makefile");

                if (makeFile == null) {
                    log.log(INFO, "Project has no Makefile");
                    return Optional.absent();
                }

                File workDir = projectInfo.getProjectDirAsFile();
                String[] command = {"make", "test"};

                final ProcessRunner runner = new ProcessRunner(command, workDir, io);

                try {
                    ProcessResult result = runner.call();
                    int ret = result.statusCode;
                    if (ret != 0) {
                        io.select();
                        log.log(INFO, "Compile resulted in non-zero exit code {0}", result.statusCode);
                        return Optional.absent();
                    }

                    log.log(INFO, "Running tests");
                    return Optional.of(runTests(projectInfo, true));
                } catch (Exception ex) {
                    io.select();
                    throw ex;
                }
            }
        };
    }

    // TODO: use make
    private TestRunResult runTests(final TmcProjectInfo projectInfo, final boolean withValgrind) throws Exception {
        log.log(INFO, "Running tests {0}", projectInfo.getProjectName());
        final File testDir = projectInfo.getProjectDirAsFile();
        String[] command;

        if (withValgrind) {
            command = new String[]{"valgrind", "--track-origins=yes", "--leak-check=full", "--log-file=valgrind.log", "."
                + File.separatorChar + "test" + File.separatorChar + "test"};
        } else {
            // If running tests with make fails - fall back running them manually
            command = new String[]{testDir.getAbsolutePath()
                + File.separatorChar + "test" + File.separatorChar + "test"};
        }
        log.log(INFO, "Running tests for project {0} with command {1}",
                new Object[]{projectInfo.getProjectName(), Arrays.deepToString(command)});

        ProcessRunner runner = new ProcessRunner(command, testDir, IOProvider.getDefault()
                .getIO(projectInfo.getProjectName(), false));

        try {
            log.info("Preparing to run tests");
            runner.call();
            log.info("Running tests completed");
        } catch (IOException e) {
            log.log(INFO, "IOException while running tests, kinda wanted. {0}", e.getMessage());
            if (withValgrind) {
                return runTests(projectInfo, false);
            } else {
                log.log(WARNING, "Failed to run tests for project: \"{0}\" with command: \"{1}\".\n\"{2}\"",
                        new Object[]{projectInfo.getProjectName(), Arrays.deepToString(command), e.getMessage()});
                throw new UserVisibleException("Failed to run tests:\n" + e.getMessage());
            }
        }

        File resultsFile = new File(testDir.getAbsolutePath() + "/tmc_test_results.xml");
        File valgrindLog = withValgrind ? new File(testDir.getAbsolutePath() + "/valgrind.log") : null;

        log.info("Locating exercise");
        Exercise exercise = projectMediator.tryGetExerciseForProject(projectInfo, courseDb);

        if (exercise != null) {
            log.log(INFO, "Parsing exercises with valgrind strategy {0}", exercise.getValgrindStrategy());
            return resultParser.parseCTestResults(resultsFile, valgrindLog, exercise.getValgrindStrategy());
        } else {
            log.log(INFO, "Parsing exercises with out valgrind strategy");
            return resultParser.parseCTestResults(resultsFile, valgrindLog, null);
        }
    }
}
