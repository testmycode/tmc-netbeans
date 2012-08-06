package fi.helsinki.cs.tmc.actions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
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
import fi.helsinki.cs.tmc.utilities.process.ProcessResult;
import fi.helsinki.cs.tmc.utilities.process.ProcessRunner;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.module.api.support.ActionUtils;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.Project;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.ActionProgress;
import org.netbeans.spi.project.ActionProvider;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

@Messages("CTL_RunTestsLocallyExerciseAction=Run &tests locally")
public class RunTestsLocallyAction extends AbstractExerciseSensitiveAction {

    private static final Logger log = Logger.getLogger(RunTestsLocallyAction.class.getName());
    
    private CourseDb courseDb;
    private ProjectMediator projectMediator;
    private TestResultDisplayer resultDisplayer;
    private ConvenientDialogDisplayer dialogDisplayer;
    private SubmitExerciseAction submitAction;

    public RunTestsLocallyAction() {
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.submitAction = new SubmitExerciseAction();
        
        putValue("noIconInMenu", Boolean.TRUE);
    }
    
    @Override
    protected CourseDb getCourseDb() {
        return courseDb;
    }

    @Override
    protected ProjectMediator getProjectMediator() {
        return projectMediator;
    }
    
    @Override
    protected void performAction(Node[] nodes) {
        performAction(projectsFromNodes(nodes).toArray(new Project[0]));
    }
    
    private void performAction(Project ... projects) {
        projectMediator.saveAllFiles();
        for (final Project project : projects) {
            final TmcProjectInfo projectInfo = projectMediator.wrapProject(project);
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
    
    private Callable<Integer> executorTaskToCallable(final ExecutorTask et) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return et.result();
            }
        };
    }
    
    private Callable<Integer> startCompilingProject(TmcProjectInfo projectInfo) {
        switch (projectInfo.getProjectType()) {
            case JAVA_SIMPLE: return startCompilingAntProject(projectInfo);
            case JAVA_MAVEN: return startCompilingMavenProject(projectInfo);
            default: throw new IllegalArgumentException("Unknown project type: " + projectInfo.getProjectType());
        }
    }
    
    private Callable<Integer> startCompilingAntProject(TmcProjectInfo projectInfo) {
        Project project = projectInfo.getProject();
        FileObject buildScript = project.getProjectDirectory().getFileObject("build.xml");
        if (buildScript == null) {
            throw new RuntimeException("Project has no build.xml");
        }
        ExecutorTask task;
        try {
            task = ActionUtils.runTarget(buildScript, new String[] { "compile-test" }, null);
            return executorTaskToCallable(task);
        } catch (IOException ex) {
            throw ExceptionUtils.toRuntimeException(ex);
        }
    }
    
    private Callable<Integer> startCompilingMavenProject(TmcProjectInfo projectInfo) {
        final Project project = projectInfo.getProject();
        
        // Ideal implementation, if the Maven Project API were public:
        //File projectDir = projectInfo.getProjectDirAsFile();
        //List<String> goals = Arrays.asList("test-compile");
        //RunConfig cfg = RunUtils.createRunConfig(projectDir, project, projectInfo.getProjectName(), goals);
        //return RunUtils.executeMaven(cfg);*/
        
        // Alternative implementation using ActionProgress, introduced in NB 7.2.
        // This calls the default maven goal, install.
        // This way we end up running our tests twice, but we can live with that for now.
        final ActionProvider actionProvider = project.getLookup().lookup(ActionProvider.class);
        
        return new Callable<Integer>() {
            @Override
            public Integer call() throws InterruptedException {
                final Semaphore semaphore = new Semaphore(0);
                ActionProgress progress = new ActionProgress() {
                    @Override
                    protected void started() {
                    }
                    @Override
                    public void finished(boolean success) {
                        // success is always true, even if the build fails (NB 7.2).
                        // It would be useless anyway since the tests can fail
                        // even if they compiled fine.
                        semaphore.release();
                    }
                };
                
                Lookup lookup = Lookups.singleton(progress);
                actionProvider.invokeAction(ActionProvider.COMMAND_BUILD, lookup);
                
                semaphore.acquire();
                return 0;
            }
        };
    }
    
    private void startRunningTests(TmcProjectInfo projectInfo) {
        FileObject testDir = findTestDir(projectInfo);
        if (testDir == null) {
            dialogDisplayer.displayError("No test directory in project");
            return;
        }

        List<TestMethod> tests = findProjectTests(projectInfo, testDir);
        startRunningTests(projectInfo, testDir, tests);
    }
    
    private List<TestMethod> findProjectTests(TmcProjectInfo projectInfo, FileObject testDir) {
        TestScanner scanner = new TestScanner();
        scanner.setClassPath(getTestClassPath(projectInfo, testDir).toString(ClassPath.PathConversionMode.WARN));
        scanner.addSource(FileUtil.toFile(testDir));
        return scanner.findTests();
    }
    
    private FileObject findTestDir(TmcProjectInfo projectInfo) {
        // Ideally we'd get these paths from NB, but let's assume the conventional ones for now.
        FileObject root = projectInfo.getProjectDir();
        switch (projectInfo.getProjectType()) {
            case JAVA_SIMPLE: return root.getFileObject("test");
            case JAVA_MAVEN: return getSubdir(root, "src", "test", "java");
            default: throw new IllegalArgumentException("Unknown project type");
        }
    }
    
    private FileObject getSubdir(FileObject fo, String ... subdirs) {
        for (String s : subdirs) {
            if (fo == null) {
                return null;
            }
            fo = fo.getFileObject(s);
        }
        return fo;
    }
    
    private void startRunningTests(TmcProjectInfo projectInfo, FileObject testDir, List<TestMethod> testMethods) {
        final Project project = projectInfo.getProject();
        
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
            
            Integer memoryLimit = getMemoryLimit(project);
            if (memoryLimit != null) {
                args.add("-Xmx" + memoryLimit + "M");
            }
            
            args.add("fi.helsinki.cs.tmc.testrunner.Main");
            
            for (int i = 0; i < testMethods.size(); ++i) {
                args.add(testMethods.get(i).toString());
            }

            InputOutput inOut = IOProvider.getDefault().getIO("test output", false);
            if (inOut.isClosed()) {
                inOut.select();
            }

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
                        log.info("Failed to run tests. Status code: " + result.statusCode);
                        dialogDisplayer.displayError("Failed to run tests.\n" + result.errorOutput);
                        return;
                    }
                    
                    String resultJson = "";
                    try {
                        Scanner reader = new Scanner(tempFileAsFinal, "UTF-8");
                        while (reader.hasNextLine()) {
                            resultJson += reader.nextLine();
                        }
                    } catch (IOException ex) {
                        dialogDisplayer.displayError("Failed to read test results", ex);
                        return;
                    } finally {
                        tempFileAsFinal.delete();
                    }
                    
                    TestCaseList testCaseRecords;
                    try {
                        testCaseRecords = parseTestResults(resultJson);
                    } catch (IllegalArgumentException ex) {
                        log.info("Empty result from test runner");
                        dialogDisplayer.displayError("Failed to read test results");
                        return;
                    }
                    
                    List<TestCaseResult> results = new ArrayList<TestCaseResult>();
                    for (TestCase tc : testCaseRecords) {
                        results.add(TestCaseResult.fromTestCaseRecord(tc));
                    }
                    
                    if (resultDisplayer.showLocalRunResult(results)) {
                        submitAction.performAction(project);
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
    
    private TestCaseList parseTestResults(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(StackTraceElement.class, new StackTraceSerializer())
                .create();
        TestCaseList results = gson.fromJson(json, TestCaseList.class);
        if (results == null) {
            throw new IllegalArgumentException("Invalid test results");
        }
        return results;
    }
    
    private void runJavaProcessInProject(TmcProjectInfo projectInfo, ClassPath classPath, String taskName, List<String> args, InputOutput inOut, BgTaskListener<ProcessResult> listener) {
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
    
    private ClassPath getTestClassPath(TmcProjectInfo projectInfo, FileObject testDir) {
        ClassPathProvider classPathProvider = projectInfo.getProject().getLookup().lookup(ClassPathProvider.class);
        
        ClassPath cp = classPathProvider.findClassPath(testDir, ClassPath.EXECUTE);
        if (cp == null) {
            throw new RuntimeException("Failed to get 'execute' classpath for project's tests");
        }
        return cp;
    }
    
    private ClassPath getTestRunnerClassPath(TmcProjectInfo projectInfo) {
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
    
    private Integer getMemoryLimit(Project project) {
        Exercise ex = projectMediator.tryGetExerciseForProject(projectMediator.wrapProject(project), courseDb);
        if (ex != null) {
            return ex.getMemoryLimit();
        } else {
            return null;
        }
    }
    
    @Override
    public String getName() {
        return "Run &tests locally";
    }
    
    @Override
    protected String iconResource() {
        // The setting in layer.xml doesn't work with NodeAction
        return "org/netbeans/modules/project/ui/resources/testProject.png";
    }
}
