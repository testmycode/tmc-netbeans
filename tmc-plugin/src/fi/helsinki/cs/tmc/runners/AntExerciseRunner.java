package fi.helsinki.cs.tmc.runners;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.TestRunResult;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.UserVisibleException;
import fi.helsinki.cs.tmc.testscanner.TestMethod;
import fi.helsinki.cs.tmc.utilities.EmptyBgTaskListener;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import fi.helsinki.cs.tmc.utilities.process.ProcessResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;
import org.apache.tools.ant.module.api.support.ActionUtils;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.Project;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.windows.InputOutput;

public class AntExerciseRunner extends AbstractJavaExerciseRunner {

    private static final Logger log = Logger.getLogger(AntExerciseRunner.class.getName());

    @Override
    public Callable<Optional<TestRunResult>> getTestRunningTask(final TmcProjectInfo projectInfo) {
        return new Callable<Optional<TestRunResult>>() {
            @Override
            public Optional<TestRunResult> call() throws Exception {

                log.log(INFO,
                        "Starting compile");

                Project project = projectInfo.getProject();
                FileObject buildScript = project.getProjectDirectory().getFileObject("build.xml");
                if (buildScript == null) {
                    throw new RuntimeException("Project has no build.xml");
                }
                ExecutorTask task;

                try {
                    task = ActionUtils.runTarget(buildScript, new String[]{"compile-test"}, null);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                    throw ExceptionUtils.toRuntimeException(ex);
                } catch (IllegalArgumentException ex) {
                    Exceptions.printStackTrace(ex);
                    throw ExceptionUtils.toRuntimeException(ex);
                }

                int compileResult = task.result();
                if (compileResult == 0) {
                    log.log(INFO, "Compile success for project {0}", projectInfo.toString());
                    return Optional.of(runTests(projectInfo));
                } else {
                    return Optional.absent();
                }
            }
        };
    }

    protected TestRunResult runTests(final TmcProjectInfo projectInfo) throws UserVisibleException, IOException, InterruptedException, ExecutionException {

        FileObject testDir = findTestDir(projectInfo);
        if (testDir == null) {
            throw new UserVisibleException("No test directory in project");
        }

        List<TestMethod> tests = findProjectTests(projectInfo, testDir);

        File tempFile;
        tempFile = File.createTempFile("tmc_test_results", ".txt");
        try {
            return runTests(projectInfo, testDir, tests, tempFile);
        } finally {
            tempFile.delete();
        }
    }

    private TestRunResult runTests(final TmcProjectInfo projectInfo, FileObject testDir, List<TestMethod> testMethods, File tempFile) throws UserVisibleException {
        try {
            ArrayList<String> args = new ArrayList<String>();
            args.add("-Dtmc.test_class_dir=" + FileUtil.toFile(testDir).getAbsolutePath());
            args.add("-Dtmc.results_file=" + tempFile.getAbsolutePath());
            args.add("-D" + ERROR_MSG_LOCALE_SETTING + "=" + settings.getErrorMsgLocale().toString());

            if (endorsedLibsExist(projectInfo)) {
                args.add("-Djava.endorsed.dirs=" + endorsedLibsPath(projectInfo));
            }

            Exercise exercise = tryGetExercise(projectInfo.getProject());
            if (exercise != null) {
                if (exercise.getMemoryLimit() != null) {
                    args.add("-Xmx" + exercise.getMemoryLimit() + "M");
                }
                if (exercise.getRuntimeParams() != null) {
                    args.addAll(Arrays.asList(exercise.getRuntimeParams()));
                }
            }

            args.add("fi.helsinki.cs.tmc.testrunner.Main");

            for (int i = 0; i < testMethods.size(); ++i) {
                args.add(testMethods.get(i).toString());
            }
            InputOutput inOut = getIoTab();

            ClassPath classPath = getTestClassPath(projectInfo, testDir);

            Future<ProcessResult> runFuture = runJavaProcessInProject(projectInfo, classPath, "Running tests", args, inOut, EmptyBgTaskListener.get());

            ProcessResult processResult = runFuture.get();
            log.info("Test run standard output:");
            log.info(processResult.output);
            log.info("Test run error output:");
            log.info(processResult.errorOutput);

            if (processResult.statusCode != 0) {
                log.log(Level.INFO, "Failed to run tests. Status code: {0}", processResult.statusCode);
                throw new UserVisibleException("Failed to run tests.\n" + processResult.errorOutput);
            }

            return resultParser.parseTestResults(tempFile);

        } catch (CancellationException ex) {
            throw ex;
        } catch (UserVisibleException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new UserVisibleException("Failed to run tests", t);
        }
    }
}
