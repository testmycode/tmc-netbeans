package fi.helsinki.cs.tmc.actions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.data.serialization.cresultparser.CTestResultParser;
import fi.helsinki.cs.tmc.events.TmcEvent;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.testrunner.StackTraceSerializer;
import fi.helsinki.cs.tmc.testrunner.TestCase;
import fi.helsinki.cs.tmc.testrunner.TestCaseList;
import fi.helsinki.cs.tmc.testscanner.TestMethod;
import fi.helsinki.cs.tmc.testscanner.TestScanner;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import fi.helsinki.cs.tmc.utilities.maven.MavenRunBuilder;
import fi.helsinki.cs.tmc.utilities.process.ProcessResult;
import fi.helsinki.cs.tmc.utilities.process.ProcessRunner;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.module.api.support.ActionUtils;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.Project;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

@Messages("CTL_RunTestsLocallyExerciseAction=Run &tests locally")
public class RunTestsLocallyAction extends AbstractExerciseSensitiveAction {

    private static final String MAVEN_TEST_RUN_GOAL = "fi.helsinki.cs.tmc:tmc-maven-plugin:1.6:test";
    private static final String ERROR_MSG_LOCALE_SETTING = "fi.helsinki.cs.tmc.edutestutils.defaultLocale";
    private static final Logger log = Logger.getLogger(RunTestsLocallyAction.class.getName());

    public static class InvokedEvent implements TmcEvent {

        public final TmcProjectInfo projectInfo;

        public InvokedEvent(TmcProjectInfo projectInfo) {
            this.projectInfo = projectInfo;
        }
    }
    private TmcSettings settings;
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private TestResultDisplayer resultDisplayer;
    private ConvenientDialogDisplayer dialogDisplayer;
    private SubmitExerciseAction submitAction;
    private TmcEventBus eventBus;

    public RunTestsLocallyAction() {
        this.settings = TmcSettings.getDefault();
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.submitAction = new SubmitExerciseAction();
        this.eventBus = TmcEventBus.getDefault();

        putValue("noIconInMenu", Boolean.TRUE);
    }

    @Override
    protected synchronized CourseDb getCourseDb() {
        return courseDb;
    }

    @Override
    protected synchronized ProjectMediator getProjectMediator() {
        return projectMediator;
    }

    @Override
    protected synchronized void performAction(Node[] nodes) {
        performAction(projectsFromNodes(nodes).toArray(new Project[0]));
    }

    private synchronized void performAction(Project... projects) {
        projectMediator.saveAllFiles();
        for (final Project project : projects) {
            final TmcProjectInfo projectInfo = projectMediator.wrapProject(project);
            eventBus.post(new InvokedEvent(projectInfo));
            BgTask.start("Compiling project", startCompilingProject(projectInfo), new BgTaskListener<Integer>() {
                @Override
                public void bgTaskReady(Integer result) {
                    if (result == 0) {
                        startRunningTests(projectInfo);
                    } else {
                        dialogDisplayer.displayError("The code did not compile.");
                    }
                }

                @Override
                public void bgTaskFailed(Throwable ex) {
                    dialogDisplayer.displayError("Failed to compile the code.");
                }

                @Override
                public void bgTaskCancelled() {
                }
            });
        }
    }

    private synchronized Callable<Integer> executorTaskToCallable(final ExecutorTask et) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return et.result();
            }
        };
    }

    private synchronized Callable<Integer> startCompilingProject(TmcProjectInfo projectInfo) {
        switch (projectInfo.getProjectType()) {
            case JAVA_SIMPLE:
                return startCompilingAntProject(projectInfo);
            case JAVA_MAVEN:
                return startCompilingMavenProject(projectInfo);
            case MAKEFILE:
                return startCompilingMakefileProject(projectInfo);
            default:
                throw new IllegalArgumentException("Unknown project type: " + projectInfo.getProjectType());
        }
    }

    private synchronized Callable<Integer> startCompilingAntProject(TmcProjectInfo projectInfo) {
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

    private synchronized Callable<Integer> startCompilingMakefileProject(TmcProjectInfo projectInfo) {
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
            public synchronized Integer call() throws Exception {
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

    private synchronized Callable<Integer> startCompilingMavenProject(TmcProjectInfo projectInfo) {
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
            public synchronized Integer call() throws Exception {
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

    private synchronized void startRunningTests(TmcProjectInfo projectInfo) {
        switch (projectInfo.getProjectType()) {
            case JAVA_SIMPLE:
                startRunningSimpleProjectTests(projectInfo);
                break;
            case JAVA_MAVEN:
                startRunningMavenProjectTests(projectInfo);
                break;
            case MAKEFILE:
                startRunningMakefileProjectTests(projectInfo, true);
                break;
            default:
                throw new IllegalArgumentException("Unknown project type: " + projectInfo.getProjectType());
        }
    }

    private synchronized void startRunningSimpleProjectTests(TmcProjectInfo projectInfo) {
        FileObject testDir = findTestDir(projectInfo);
        if (testDir == null) {
            dialogDisplayer.displayError("No test directory in project");
            return;
        }

        List<TestMethod> tests = findProjectTests(projectInfo, testDir);
        startRunningSimpleProjectTests(projectInfo, testDir, tests);
    }

    private synchronized void startRunningMakefileProjectTests(final TmcProjectInfo projectInfo, final boolean withValgrind) {
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
            public synchronized void bgTaskCancelled() {
            }

            @Override
            public synchronized void bgTaskFailed(Throwable ex) {
                if (withValgrind) {
                    startRunningMakefileProjectTests(projectInfo, false);
                } else {
                    dialogDisplayer.displayError("Failed to run tests:\n" + ex.getMessage());
                }
            }
        });

    }

    private synchronized void startRunningMavenProjectTests(final TmcProjectInfo projectInfo) {
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
            public synchronized void bgTaskReady(ProcessResult processResult) {
                File resultsFile = new File(
                        projectDir.getPath() + File.separator
                        + "target" + File.separator
                        + "test_output.txt");
                log.log(Level.INFO, "Next calling handleTestResults: projectInfo: {0}, file: {1}", new Object[]{projectInfo.getProjectDirAbsPath(), resultsFile.exists()});
                handleTestResults(projectInfo, resultsFile);
            }

            @Override
            public synchronized void bgTaskCancelled() {
            }

            @Override
            public synchronized void bgTaskFailed(Throwable ex) {
                dialogDisplayer.displayError("Failed to run tests:\n" + ex.getMessage());
            }
        });
    }

    private synchronized List<TestMethod> findProjectTests(TmcProjectInfo projectInfo, FileObject testDir) {
        TestScanner scanner = new TestScanner();
        scanner.setClassPath(getTestClassPath(projectInfo, testDir).toString(ClassPath.PathConversionMode.WARN));
        scanner.addSource(FileUtil.toFile(testDir));
        return scanner.findTests();
    }

    private synchronized FileObject findTestDir(TmcProjectInfo projectInfo) {
        // Ideally we'd get these paths from NB, but let's assume the conventional ones for now.
        FileObject root = projectInfo.getProjectDir();
        switch (projectInfo.getProjectType()) {
            case JAVA_SIMPLE:
                return root.getFileObject("test");
            case JAVA_MAVEN:
                return getSubdir(root, "src", "test", "java");
            default:
                throw new IllegalArgumentException("Unknown project type");
        }
    }

    private synchronized FileObject getSubdir(FileObject fo, String... subdirs) {
        for (String s : subdirs) {
            if (fo == null) {
                return null;
            }
            fo = fo.getFileObject(s);
        }
        return fo;
    }

    private synchronized boolean endorsedLibsExist(final TmcProjectInfo projectInfo) {
        File endorsedDir = endorsedLibsPath(projectInfo);
        return endorsedDir.exists() && endorsedDir.isDirectory();
    }

    private synchronized File endorsedLibsPath(final TmcProjectInfo projectInfo) {
        String path = FileUtil.toFile(projectInfo.getProjectDir()).getAbsolutePath() + File.separatorChar
                + "lib" + File.separatorChar
                + "endorsed";
        return new File(path);
    }

    private synchronized void startRunningSimpleProjectTests(final TmcProjectInfo projectInfo, FileObject testDir, List<TestMethod> testMethods) {
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
                public synchronized void bgTaskReady(ProcessResult result) {
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
                        handleTestResults(projectInfo, tempFileAsFinal);
                    } finally {
                        tempFileAsFinal.delete();
                    }
                }

                @Override
                public synchronized void bgTaskCancelled() {
                    tempFileAsFinal.delete();
                }

                @Override
                public synchronized void bgTaskFailed(Throwable ex) {
                    tempFileAsFinal.delete();
                    dialogDisplayer.displayError("Failed to run tests", ex);
                }
            });

        } catch (Exception ex) {
            tempFile.delete();
            dialogDisplayer.displayError("Failed to run tests", ex);
        }
    }

    private synchronized void handleTestResults(final TmcProjectInfo projectInfo, File resultsFile) {
        List<TestCaseResult> results;
        try {
            String resultJson = FileUtils.readFileToString(resultsFile, "UTF-8");
            results = parseTestResults(resultJson);
        } catch (Exception ex) {
            log.log(Level.WARNING, "Failed to read test results: {0}", ex.getStackTrace());
            dialogDisplayer.displayError("Failed to read test results", ex);
            return;
        }
        boolean canSubmit = submitAction.enable(projectInfo.getProject());
        resultDisplayer.showLocalRunResult(results, canSubmit, new Runnable() {
            @Override
            public synchronized void run() {
                submitAction.performAction(projectInfo.getProject());
            }
        });




    }

    private synchronized List<TestCaseResult> parseTestResults(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(StackTraceElement.class, new StackTraceSerializer())
                .create();

        TestCaseList testCaseRecords = gson.fromJson(json, TestCaseList.class);
        if (testCaseRecords
                == null) {
            String msg = "Empty result from test runner";
            log.warning(msg);
            throw new IllegalArgumentException(msg);
        }
        List<TestCaseResult> results = new ArrayList<TestCaseResult>();
        for (TestCase tc : testCaseRecords) {
            results.add(TestCaseResult.fromTestCaseRecord(tc));
        }
        return results;
    }

    private synchronized void runJavaProcessInProject(TmcProjectInfo projectInfo, ClassPath classPath, String taskName, List<String> args, InputOutput inOut, BgTaskListener<ProcessResult> listener) {
        FileObject projectDir = projectInfo.getProjectDir();

        JavaPlatform platform = JavaPlatform.getDefault(); // Should probably use project's configured platform instead

        FileObject javaExe = platform.findTool("java");
        if (javaExe == null) {
            throw new IllegalArgumentException();
        }

        // TMC server packages this with every exercise for our convenience.
        // True even for Maven exercises, at least until NB's Maven API is published.
        ClassPath testRunnerClassPath = getTestRunnerClassPath(projectInfo);

        if (testRunnerClassPath != null) {
            classPath = ClassPathSupport.createProxyClassPath(classPath, testRunnerClassPath);
        }

        String[] command = new String[3 + args.size()];
        command[0] = FileUtil.toFile(javaExe).getAbsolutePath();
        command[1] = "-cp";
        command[2] = classPath.toString(ClassPath.PathConversionMode.WARN);
        System.arraycopy(args.toArray(new String[args.size()]), 0, command, 3, args.size());

        log.info(StringUtils.join(command, ' '));
        ProcessRunner runner = new ProcessRunner(command, FileUtil.toFile(projectDir), inOut);
        BgTask.start(taskName, runner, listener);




    }

    private synchronized ClassPath getTestClassPath(TmcProjectInfo projectInfo, FileObject testDir) {
        ClassPathProvider classPathProvider = projectInfo.getProject().getLookup().lookup(ClassPathProvider.class);

        if (classPathProvider
                == null) {
            throw new RuntimeException("Project's class path not (yet) initialized");
        }
        ClassPath cp = classPathProvider.findClassPath(testDir, ClassPath.EXECUTE);
        if (cp
                == null) {
            throw new RuntimeException("Failed to get 'execute' classpath for project's tests");
        }
        return cp;
    }

    private synchronized ClassPath getTestRunnerClassPath(TmcProjectInfo projectInfo) {
        FileObject projectDir = projectInfo.getProjectDir();
        FileObject testrunnerDir = projectDir.getFileObject("lib/testrunner");
        if (testrunnerDir != null) {
            FileObject[] files = testrunnerDir.getChildren();
            ArrayList<URL> urls = new ArrayList<URL>();
            for (FileObject file : files) {
                URL url = FileUtil.urlForArchiveOrDir(FileUtil.toFile(file));
                if (url != null) {
                    urls.add(url);
                }
            }
            return ClassPathSupport.createClassPath(urls.toArray(new URL[0]));
        } else {
            return null;
        }
    }

    private synchronized Integer getMemoryLimit(Project project) {
        Exercise ex = projectMediator.tryGetExerciseForProject(projectMediator.wrapProject(project), courseDb);
        if (ex != null) {
            return ex.getMemoryLimit();
        } else {
            return null;
        }
    }

    @Override
    public synchronized String getName() {
        return "Run &tests locally";
    }

    @Override
    protected synchronized String iconResource() {
        // The setting in layer.xml doesn't work with NodeAction
        return "org/netbeans/modules/project/ui/resources/testProject.png";
    }

    @Override
    protected synchronized boolean enabledFor(Exercise exercise) {
        // Overridden to not care about the deadline
        return exercise.isReturnable();
    }

    private synchronized InputOutput getIoTab() {
        InputOutput inOut = IOProvider.getDefault().getIO("Test output", false);
        try {
            inOut.getOut().reset();
        } catch (IOException e) {
            // Ignore
        }
        if (inOut.isClosed()) {
            inOut.select();
        }
        return inOut;
    }
}
