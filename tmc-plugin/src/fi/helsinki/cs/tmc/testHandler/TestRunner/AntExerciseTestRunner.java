package fi.helsinki.cs.tmc.testHandler.TestRunner;

import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.testscanner.TestMethod;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import fi.helsinki.cs.tmc.utilities.process.ProcessResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import org.apache.tools.ant.module.api.support.ActionUtils;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.Project;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.windows.InputOutput;

public class AntExerciseTestRunner extends AbstractExerciseTestRunner {

    public AntExerciseTestRunner() {
        super();
    }

    @Override
    public Callable<Integer> startCompilingProject(TmcProjectInfo projectInfo) {

        Project project = projectInfo.getProject();
        FileObject buildScript = project.getProjectDirectory().getFileObject("build.xml");
        if (buildScript == null) {
            throw new RuntimeException("Project has no build.xml");
        }
        ExecutorTask task;
        try {
            task = ActionUtils.runTarget(buildScript, new String[]{"compile-test"}, null);
            return executorTaskToCallable(task);
        } catch (IOException ex) {
            throw ExceptionUtils.toRuntimeException(ex);
        }
    }

    @Override
    public void startRunningTests(TmcProjectInfo projectInfo) {
        FileObject testDir = findTestDir(projectInfo);
        if (testDir == null) {
            dialogDisplayer.displayError("No test directory in project");
            return;
        }

        List<TestMethod> tests = findProjectTests(projectInfo, testDir);
        startRunningSimpleProjectTests(projectInfo, testDir, tests);
    }

    private void startRunningSimpleProjectTests(final TmcProjectInfo projectInfo, FileObject testDir, List<TestMethod> testMethods) {
        File tempFile;
        try {
            tempFile = File.createTempFile("tmc_test_results", ".txt");
        } catch (IOException ex) {
            dialogDisplayer.displayError("Failed to create temporary file for test results.", ex);
            return;
        }

        try {
            ArrayList<String> args = new ArrayList<String>();
            args.add("-Dtmc.test_class_dir=" + FileUtil.toFile(testDir).getAbsolutePath());
            args.add("-Dtmc.results_file=" + tempFile.getAbsolutePath());
            args.add("-D" + ERROR_MSG_LOCALE_SETTING + "=" + settings.getErrorMsgLocale().toString());

            if (endorsedLibsExist(projectInfo)) {
                args.add("-Djava.endorsed.dirs=" + endorsedLibsPath(projectInfo));
            }

            Integer memoryLimit = getMemoryLimit(projectInfo.getProject());
            if (memoryLimit != null) {
                args.add("-Xmx" + memoryLimit + "M");
            }

            args.add("fi.helsinki.cs.tmc.testrunner.Main");

            for (int i = 0; i < testMethods.size(); ++i) {
                args.add(testMethods.get(i).toString());
            }
            InputOutput inOut = getIoTab();

            final File tempFileAsFinal = tempFile;
            ClassPath classPath = getTestClassPath(projectInfo, testDir);
            runJavaProcessInProject(projectInfo, classPath, "Running tests", args, inOut, new BgTaskListener<ProcessResult>() {
                @Override
                public void bgTaskReady(ProcessResult result) {
                    log.info("Test run standard output:");
                    log.info(result.output);
                    log.info("Test run error output:");
                    log.info(result.errorOutput);

                    if (result.statusCode != 0) {
                        log.log(Level.INFO, "Failed to run tests. Status code: {0}", result.statusCode);
                        dialogDisplayer.displayError("Failed to run tests.\n" + result.errorOutput);
                        tempFileAsFinal.delete();
                        return;
                    }

                    try {
                        javaTestResultsHandler.handle(projectInfo, tempFileAsFinal);
                    } finally {
                        tempFileAsFinal.delete();
                    }
                }

                @Override
                public void bgTaskCancelled() {
                    tempFileAsFinal.delete();
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                    tempFileAsFinal.delete();
                    dialogDisplayer.displayError("Failed to run tests", ex);
                }
            });

        } catch (Exception ex) {
            tempFile.delete();
            dialogDisplayer.displayError("Failed to run tests", ex);
        }
    }
}
