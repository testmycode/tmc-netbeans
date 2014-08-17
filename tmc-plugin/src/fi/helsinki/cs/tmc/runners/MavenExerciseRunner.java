package fi.helsinki.cs.tmc.runners;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.TestRunResult;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.utilities.maven.MavenRunBuilder;
import fi.helsinki.cs.tmc.utilities.process.ProcessResult;
import fi.helsinki.cs.tmc.utilities.process.ProcessRunner;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public class MavenExerciseRunner extends AbstractJavaExerciseRunner {
    private static final String MAVEN_TEST_RUN_GOAL = "fi.helsinki.cs.tmc:tmc-maven-plugin:1.6:test";

    @Override
    public Callable<Integer> getCompilingTask(TmcProjectInfo projectInfo) {
        File projectDir = projectInfo.getProjectDirAsFile();

        String goal = "test-compile";
        final InputOutput inOut = IOProvider.getDefault().getIO(projectInfo.getProjectName(), false);

        final ProcessRunner runner = new MavenRunBuilder()
                .setProjectDir(projectDir)
                .addGoal(goal)
                .setIO(inOut)
                .createProcessRunner();

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
    public Callable<TestRunResult> getTestRunningTask(final TmcProjectInfo projectInfo) {
        final File projectDir = projectInfo.getProjectDirAsFile();
        String goal = MAVEN_TEST_RUN_GOAL;
        Map<String, String> props = new HashMap<String, String>();
        InputOutput inOut = getIoTab();

        List<String> jvmOpts = new ArrayList<String>();

        Exercise exercise = tryGetExercise(projectInfo.getProject());
        if (exercise != null) {
            if (exercise.getMemoryLimit() != null) {
                jvmOpts.add("-Xmx" + exercise.getMemoryLimit() + "M");
            }
            if (exercise.getRuntimeParams() != null) {
                jvmOpts.addAll(Arrays.asList(exercise.getRuntimeParams()));
            }
        }

        jvmOpts.add("-D" + ERROR_MSG_LOCALE_SETTING + "=" + settings.getErrorMsgLocale().toString());

        props.put("tmc.test.jvm_opts", StringUtils.join(jvmOpts, ' '));

        final ProcessRunner runner = new MavenRunBuilder()
                .setProjectDir(projectDir)
                .addGoal(goal)
                .setProperties(props)
                .setIO(inOut)
                .createProcessRunner();

        return new Callable<TestRunResult>() {
            @Override
            public TestRunResult call() throws Exception {
                runner.call();
                File resultsFile = new File(
                        projectDir.getPath() + File.separator
                        + "target" + File.separator
                        + "test_output.txt");
                return resultParser.parseTestResults(resultsFile);
            }
        };
    }
}
