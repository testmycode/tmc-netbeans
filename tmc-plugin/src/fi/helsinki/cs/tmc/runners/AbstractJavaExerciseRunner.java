package fi.helsinki.cs.tmc.runners;

import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import static fi.helsinki.cs.tmc.runners.AbstractExerciseRunner.log;
import fi.helsinki.cs.tmc.testscanner.TestMethod;
import fi.helsinki.cs.tmc.testscanner.TestScanner;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.process.ProcessResult;
import fi.helsinki.cs.tmc.utilities.process.ProcessRunner;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.windows.InputOutput;

public abstract class AbstractJavaExerciseRunner extends AbstractExerciseRunner {

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

    protected Future<ProcessResult> runJavaProcessInProject(TmcProjectInfo projectInfo, ClassPath classPath, String taskName, List<String> args, InputOutput inOut, BgTaskListener<? super ProcessResult> listener) {
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
        return BgTask.start(taskName, runner, listener);
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

    protected List<TestMethod> findProjectTests(TmcProjectInfo projectInfo, FileObject testDir) {
        TestScanner scanner = new TestScanner(loadJavaCompiler());
        scanner.setClassPath(getTestClassPath(projectInfo, testDir).toString(ClassPath.PathConversionMode.WARN));
        scanner.addSource(FileUtil.toFile(testDir));
        return scanner.findTests();
    }

    private JavaCompiler loadJavaCompiler() {
        // https://netbeans.org/bugzilla/show_bug.cgi?id=203540
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(JavaPlatform.class.getClassLoader());
            return ToolProvider.getSystemJavaCompiler();
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
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

    private FileObject getSubdir(FileObject fo, String... subdirs) {
        for (String s : subdirs) {
            if (fo == null) {
                return null;
            }
            fo = fo.getFileObject(s);
        }
        return fo;
    }
}
