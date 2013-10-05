/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.testRunner;

import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.data.serialization.cresultparser.CTestResultParser;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.process.ProcessResult;
import fi.helsinki.cs.tmc.utilities.process.ProcessRunner;
import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author jamo
 */
public class MakeFileExerciseTestRunner extends AbstractExerciseTestRunner {

    public MakeFileExerciseTestRunner(){
        super();
    }
    @Override
    Callable<Integer> startCompilingProject(TmcProjectInfo projectInfo) {
           /* This solution is pretty much copied from the pre-existing Maven option.
         * I have no idea how well it will work, but this is a start.
         * --kviiri */
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
    void startRunningTests(final TmcProjectInfo projectInfo) {
        startRunningTests(projectInfo, true);
    
}
    void startRunningTests(final TmcProjectInfo projectInfo, final boolean withValgrind) {
        final File testDir = projectInfo.getProjectDirAsFile();
        String[] command;
        if (withValgrind) {
            command = new String[]{"valgrind", "--log-file=valgrind.log", "."
                + File.separatorChar + "test" + File.separatorChar + "test"};
        } else {
            //Todo: why does this need testDir.getAbsolutePath()? --kviiri
            command = new String[]{testDir.getAbsolutePath()
                + File.separatorChar + "test" + File.separatorChar + "test"};
        }
        ProcessRunner runner = new ProcessRunner(command, testDir, IOProvider.getDefault()
                .getIO(projectInfo.getProjectName(), false));

        BgTask.start(
                "Running tests", runner, new BgTaskListener<ProcessResult>() {
            @Override
            public void bgTaskReady(ProcessResult result) {
                CTestResultParser parser = new CTestResultParser(
                        new File(testDir.getAbsolutePath() + "/tmc_test_results.xml"),
                        withValgrind ? new File(testDir.getAbsolutePath() + "/valgrind.log") : null,
                        null);
                try {
                    parser.parseTestOutput();
                } catch (Exception e) {
                    dialogDisplayer.displayError("Failed to read test results:\n" + e.getClass() + " " + e.getMessage());
                    return;
                }
                boolean canSubmit = submitAction.enable(projectInfo.getProject());
                List<TestCaseResult> results = parser.getTestCaseResults();
                resultDisplayer.showLocalRunResult(results, canSubmit, new Runnable() {
                    @Override
                    public void run() {
                        submitAction.performAction(projectInfo.getProject());
                    }
                });
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                if (withValgrind) {
                    startRunningTests(projectInfo, false);
                } else {
                    dialogDisplayer.displayError("Failed to run tests:\n" + ex.getMessage());
                }
            }
        });
    }
    
}
