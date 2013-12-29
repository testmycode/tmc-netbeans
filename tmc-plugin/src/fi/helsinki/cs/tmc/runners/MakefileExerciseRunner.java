package fi.helsinki.cs.tmc.runners;

import fi.helsinki.cs.tmc.data.TestRunResult;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.UserVisibleException;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.process.ProcessResult;
import fi.helsinki.cs.tmc.utilities.process.ProcessRunner;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public class MakefileExerciseRunner extends AbstractExerciseRunner {

    @Override
    public Callable<Integer> getCompilingTask(TmcProjectInfo projectInfo) {
        Project project = projectInfo.getProject();
        FileObject makeFile = project.getProjectDirectory().getFileObject("Makefile");
        File workDir = projectInfo.getProjectDirAsFile();

        if (makeFile == null) {
            throw new RuntimeException("Project has no Makefile");
        }
        String[] command = {"make", "test"};

        final InputOutput io = IOProvider.getDefault().getIO(projectInfo.getProjectName(), false);
        final ProcessRunner runner = new ProcessRunner(command, workDir, io);
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                try {
                    ProcessResult result = runner.call();
                    int ret = result.statusCode;
                    if (ret != 0) {
                        io.select();
                    }
                    return ret;
                } catch (Exception ex) {
                    io.select();
                    throw ex;
                }
            }
        };
    }

    @Override
    public Callable<TestRunResult> getTestRunningTask(final TmcProjectInfo projectInfo) {
        return new Callable<TestRunResult>() {
            @Override
            public TestRunResult call() throws Exception {
                return runTests(projectInfo, true);
            }
        };
    }

    private TestRunResult runTests(final TmcProjectInfo projectInfo, final boolean withValgrind) throws Exception {
        final File testDir = projectInfo.getProjectDirAsFile();
        String[] command;
        if (withValgrind) {
            command = new String[]{"valgrind", "--log-file=valgrind.log", "."
                + File.separatorChar + "test" + File.separatorChar + "test"};
        } else {
            command = new String[]{testDir.getAbsolutePath()
                + File.separatorChar + "test" + File.separatorChar + "test"};
        }

        ProcessRunner runner = new ProcessRunner(command, testDir, IOProvider.getDefault()
                .getIO(projectInfo.getProjectName(), false));

        try {
            runner.call();
        } catch (IOException ex) {
            if (withValgrind) {
                runTests(projectInfo, false);
            } else {
                throw new UserVisibleException("Failed to run tests:\n" + ex.getMessage());
            }
        }

        File resultsFile = new File(testDir.getAbsolutePath() + "/tmc_test_results.xml");
        File valgrindLog = withValgrind ? new File(testDir.getAbsolutePath() + "/valgrind.log") : null;

        return resultParser.parseCTestResults(resultsFile, valgrindLog);
    }
}
