package fi.helsinki.cs.tmc.runners;

import hy.tmc.core.domain.Exercise;
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
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public class MavenExerciseRunner extends AbstractJavaExerciseRunner {

    private static final String MAVEN_TEST_RUN_GOAL = "fi.helsinki.cs.tmc:tmc-maven-plugin:1.6:test";
    private static final Logger log = Logger.getLogger(MavenExerciseRunner.class.getName());

    @Override
    public Callable<TestRunResult> getTestRunningTask(final TmcProjectInfo projectInfo) {
        final InputOutput inOut = IOProvider.getDefault().getIO(projectInfo.getProjectName(), false);

        return new Callable<TestRunResult>() {
            @Override
            public TestRunResult call() throws Exception {
                File projectDir = projectInfo.getProjectDirAsFile();
                log.info("Starting compile");
                String goal = "test-compile";

                final ProcessRunner runner = new MavenRunBuilder()
                        .setProjectDir(projectDir)
                        .addGoal(goal)
                        .setIO(inOut)
                        .createProcessRunner();
                try {
                    ProcessResult result = runner.call();
                    int ret = result.statusCode;
                    if (ret != 0) {
                        inOut.select();
                        log.log(Level.INFO, "Compile resulted in non-zero exit code {0}", result.statusCode);
                        return new TestRunResult(false);
                    } else {
                        log.log(Level.INFO, "Running tests");
                        return runTests(projectInfo, inOut);
                    }
                } catch (Exception ex) {
                    inOut.select();
                    throw ex;
                }
            }

        };
    }

    public TestRunResult runTests(final TmcProjectInfo projectInfo, InputOutput inOut) throws Exception {
        final File projectDir = projectInfo.getProjectDirAsFile();
        String goal = MAVEN_TEST_RUN_GOAL;
        Map<String, String> props = new HashMap<String, String>();
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

        runner.call();
        File resultsFile = new File(
                projectDir.getPath() + File.separator
                + "target" + File.separator
                + "test_output.txt");
        return resultParser.parseTestResults(resultsFile);
    }
}
