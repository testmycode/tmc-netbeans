package fi.helsinki.cs.tmc.testHandler.TestRunner;

import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.maven.MavenRunBuilder;
import fi.helsinki.cs.tmc.utilities.process.ProcessResult;
import fi.helsinki.cs.tmc.utilities.process.ProcessRunner;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public class MavenExerciseTestRunner extends AbstractExerciseTestRunner {

    private static final String MAVEN_TEST_RUN_GOAL = "fi.helsinki.cs.tmc:tmc-maven-plugin:1.6:test";

    public MavenExerciseTestRunner() {
        super();
    }

    @Override
    public Callable<Integer> startCompilingProject(TmcProjectInfo projectInfo) {
        File projectDir = projectInfo.getProjectDirAsFile();

        String goal = "test-compile";
        final InputOutput inOut = IOProvider.getDefault().getIO(projectInfo.getProjectName(), false);

        final ProcessRunner runner = new MavenRunBuilder()
                .setProjectDir(projectDir)
                .addGoal(goal)
                .setIO(inOut)
                .createProcessRunner();
        log.log(Level.INFO, "ERR: {0}", inOut.getErr().toString());
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                try {
                    ProcessResult result = runner.call();
                    int ret = result.statusCode;
                    if (ret != 0) {
                        inOut.select();
                    }
                    return ret;
                } catch (Exception ex) {
                    inOut.select();
                    throw ex;
                }
            }
        };
    }

    @Override
    public void startRunningTests(final TmcProjectInfo projectInfo) {
        final File projectDir = projectInfo.getProjectDirAsFile();
        String goal = MAVEN_TEST_RUN_GOAL;
        Map<String, String> props = new HashMap<String, String>();
        InputOutput inOut = getIoTab();

        List<String> jvmOpts = new ArrayList<String>();

        Integer memLimit = getMemoryLimit(projectInfo.getProject());
        if (memLimit != null) {
            jvmOpts.add("-Xmx" + memLimit + "m");
        }

        jvmOpts.add("-D" + ERROR_MSG_LOCALE_SETTING + "=" + settings.getErrorMsgLocale().toString());

        props.put("tmc.test.jvm_opts", StringUtils.join(jvmOpts, ' '));

        final ProcessRunner runner = new MavenRunBuilder()
                .setProjectDir(projectDir)
                .addGoal(goal)
                .setProperties(props)
                .setIO(inOut)
                .createProcessRunner();

        BgTask.start("Running tests", runner, new BgTaskListener<ProcessResult>() {
            @Override
            public void bgTaskReady(ProcessResult processResult) {
                File resultsFile = new File(
                        projectDir.getPath() + File.separator
                        + "target" + File.separator
                        + "test_output.txt");
                log.log(Level.INFO, "Next calling handleTestResults: projectInfo: {0}, file: {1}", new Object[]{projectInfo.getProjectDirAbsPath(), resultsFile.exists()});
                javaTestResultsHandler.handle(projectInfo, resultsFile);
//                handleTestResults(projectInfo, resultsFile);
            }

            @Override
            public void bgTaskCancelled() {
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                dialogDisplayer.displayError("Failed to run tests:\n" + ex.getMessage());
            }
        });
    }
}
