package fi.helsinki.cs.tmc.testHandler.TestRunner;

import fi.helsinki.cs.tmc.actions.RunTestsLocallyAction;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.testHandler.testResultsHandler.JavaTestResultsHandler;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.Project;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public abstract class AbstractExerciseTestRunner {

    protected static final String ERROR_MSG_LOCALE_SETTING = "fi.helsinki.cs.tmc.edutestutils.defaultLocale";
    protected static final Logger log = Logger.getLogger(RunTestsLocallyAction.class.getName());

    public abstract Callable<Integer> startCompilingProject(TmcProjectInfo projectInfo);

    public abstract void startRunningTests(TmcProjectInfo projectInfo);
    protected TmcSettings settings;
    protected CourseDb courseDb;
    protected ProjectMediator projectMediator;
    protected TestResultDisplayer resultDisplayer;
    protected ConvenientDialogDisplayer dialogDisplayer;
    protected TmcEventBus eventBus;
    protected JavaTestResultsHandler javaTestResultsHandler;

    public AbstractExerciseTestRunner() {
        this.settings = TmcSettings.getDefault();
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
        this.javaTestResultsHandler = new JavaTestResultsHandler();
    }

    protected boolean endorsedLibsExist(final TmcProjectInfo projectInfo) {
        File endorsedDir = endorsedLibsPath(projectInfo);
        return endorsedDir.exists() && endorsedDir.isDirectory();
    }

    protected File endorsedLibsPath(final TmcProjectInfo projectInfo) {
        String path = FileUtil.toFile(projectInfo.getProjectDir()).getAbsolutePath() + File.separatorChar
                + "lib" + File.separatorChar
                + "endorsed";
        return new File(path);
    }

    protected void runJavaProcessInProject(TmcProjectInfo projectInfo, ClassPath classPath, String taskName, List<String> args, InputOutput inOut, BgTaskListener<ProcessResult> listener) {
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

    protected ClassPath getTestClassPath(TmcProjectInfo projectInfo, FileObject testDir) {
        ClassPathProvider classPathProvider = projectInfo.getProject().getLookup().lookup(ClassPathProvider.class);

        if (classPathProvider == null) {
            throw new RuntimeException("Project's class path not (yet) initialized");
        }
        ClassPath cp = classPathProvider.findClassPath(testDir, ClassPath.EXECUTE);
        if (cp == null) {
            throw new RuntimeException("Failed to get 'execute' classpath for project's tests");
        }
        return cp;
    }

    protected ClassPath getTestRunnerClassPath(TmcProjectInfo projectInfo) {
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

    protected Integer getMemoryLimit(Project project) {
        Exercise ex = projectMediator.tryGetExerciseForProject(projectMediator.wrapProject(project), courseDb);
        if (ex != null) {
            return ex.getMemoryLimit();
        } else {
            return null;
        }
    }

    protected InputOutput getIoTab() {
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

    protected List<TestMethod> findProjectTests(TmcProjectInfo projectInfo, FileObject testDir) {
        TestScanner scanner = new TestScanner();
        scanner.setClassPath(getTestClassPath(projectInfo, testDir).toString(ClassPath.PathConversionMode.WARN));
        scanner.addSource(FileUtil.toFile(testDir));
        return scanner.findTests();
    }

    protected FileObject findTestDir(TmcProjectInfo projectInfo) {
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

    protected FileObject getSubdir(FileObject fo, String... subdirs) {
        for (String s : subdirs) {
            if (fo == null) {
                return null;
            }
            fo = fo.getFileObject(s);
        }
        return fo;
    }

    protected Callable<Integer> executorTaskToCallable(final ExecutorTask et) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return et.result();
            }
        };
    }
}