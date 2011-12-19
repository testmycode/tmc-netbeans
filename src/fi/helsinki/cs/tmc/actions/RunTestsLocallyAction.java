package fi.helsinki.cs.tmc.actions;

import com.google.gson.Gson;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.testrunner.TestCase;
import fi.helsinki.cs.tmc.testrunner.TestCaseList;
import fi.helsinki.cs.tmc.testscanner.TestMethod;
import fi.helsinki.cs.tmc.testscanner.TestScanner;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.process.ProcessResult;
import fi.helsinki.cs.tmc.utilities.process.ProcessRunner;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import org.apache.tools.ant.module.api.support.ActionUtils;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.Project;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;

@Messages("CTL_RunTestsLocallyExerciseAction=Run &tests locally")
public class RunTestsLocallyAction extends AbstractTmcRunAction {

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
            BgTask.start("Compiling project", new BgTaskListener<Integer>() {
                @Override
                public void bgTaskReady(Integer result) {
                    if (result == 0) {
                        startRunningTests(project);
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
                
            }, new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return startCompilingProject(project).result();
                }
            });
        }
    }
    
    private ExecutorTask startCompilingProject(Project project) throws IOException {
        FileObject buildScript = project.getProjectDirectory().getFileObject("build.xml");
        if (buildScript == null) {
            throw new RuntimeException("Project has no build.xml");
        }
        return ActionUtils.runTarget(buildScript, new String[] { "compile-test" }, null);
    }
    
    private List<TestMethod> findProjectTests(Project project, FileObject testDir) {
        TestScanner scanner = new TestScanner();
        scanner.setClassPath(getTestClassPath(project).toString(ClassPath.PathConversionMode.WARN));
        scanner.addSource(FileUtil.toFile(testDir));
        return scanner.findTests();
    }
    
    private void startRunningTests(Project project) {
        FileObject testDir = project.getProjectDirectory().getFileObject("test");
        if (testDir == null) {
            dialogDisplayer.displayError("No test directory in project");
            return;
        }

        List<TestMethod> tests = findProjectTests(project, testDir);
        startRunningTests(project, testDir, tests);
    }
    
    private void startRunningTests(final Project project, FileObject testDir, List<TestMethod> testMethods) {
        File tempFile;
        try {
            tempFile = File.createTempFile("tmc_test_results", ".txt");
        } catch (IOException ex) {
            dialogDisplayer.displayError("Failed to create temporary file for test results.", ex);
            return;
        }
        
        try {
            final int argsBeforeMethods = 3;
            String[] args = new String[argsBeforeMethods + testMethods.size()];
            args[0] = "-Dtmc.test_class_dir=" + FileUtil.toFile(testDir).getAbsolutePath();
            args[1] = "-Dtmc.results_file=" + tempFile.getAbsolutePath();
            args[2] = "fi.helsinki.cs.tmc.testrunner.Main";
            for (int i = 0; i < testMethods.size(); ++i) {
                args[argsBeforeMethods + i] = testMethods.get(i).toString();
            }
            
            //TODO: I/O to the output window!
            
            final File tempFileAsFinal = tempFile;
            runJavaProcessInProject(project, "Running tests", args, new BgTaskListener<ProcessResult>() {
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
        TestCaseList results = new Gson().fromJson(json, TestCaseList.class);
        if (results == null) {
            throw new IllegalArgumentException("Invalid test results");
        }
        return results;
    }
    
    private void runJavaProcessInProject(Project project, String taskName, String[] args, BgTaskListener<ProcessResult> listener) {
        FileObject projectDir = project.getProjectDirectory();
        
        JavaPlatform platform = JavaPlatform.getDefault(); // TODO: should use project's configured platform instead
        ClassPath classPath = getTestClassPath(project);
        
        FileObject javaExe = platform.findTool("java");
        if (javaExe == null) {
            throw new IllegalArgumentException();
        }
        
        String[] command = new String[3 + args.length];
        command[0] = FileUtil.toFile(javaExe).getAbsolutePath();
        command[1] = "-cp";
        command[2] = classPath.toString(ClassPath.PathConversionMode.WARN);
        System.arraycopy(args, 0, command, 3, args.length);
        
        ProcessRunner runner = new ProcessRunner(command, FileUtil.toFile(projectDir));
        BgTask.start(taskName, listener, runner);
    }
    
    private ClassPath getTestClassPath(Project project) throws RuntimeException {
        FileObject projectDir = project.getProjectDirectory();
        ClassPathProvider classPathProvider = project.getLookup().lookup(ClassPathProvider.class);
        
        ClassPath cp = classPathProvider.findClassPath(projectDir.getFileObject("test"), ClassPath.EXECUTE);
        if (cp == null) {
            throw new RuntimeException("Failed to get 'execute' classpath for project's tests");
        }
        return cp;
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
