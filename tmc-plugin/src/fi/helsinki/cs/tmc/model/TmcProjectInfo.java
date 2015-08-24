package fi.helsinki.cs.tmc.model;

import com.google.common.annotations.VisibleForTesting;
import fi.helsinki.cs.tmc.utilities.zip.RecursiveZipper;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Carries information about a project used in TMC.
 */
public class TmcProjectInfo {

    private Project project;

    /*package*/ TmcProjectInfo(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public String getProjectName() {
        return ProjectUtils.getInformation(project).getDisplayName();
    }

    public FileObject getProjectDir() {
        return project.getProjectDirectory();
    }

    public File getProjectDirAsFile() {
        return FileUtil.toFile(getProjectDir());
    }

    public Path getProjectDirAsPath() {
        return Paths.get(getProjectDirAbsPath());
    }

    public String getProjectDirAbsPath() {
        return FileUtil.toFile(getProjectDir()).getAbsolutePath();
    }

    public boolean isOpen() {
        return OpenProjects.getDefault().isProjectOpen(project);
    }

    public TmcProjectFile getTmcProjectFile() {
        return TmcProjectFile.forProject(FileUtil.toFile(getProjectDir()));
    }

    //TODO: a more robust/elegant/extensible project type recognition system
    @Deprecated
    public TmcProjectType getProjectType() {
        String pd = getProjectDirAbsPath();
        if (new File(pd + File.separatorChar + "pom.xml").exists()) {
            return TmcProjectType.JAVA_MAVEN;
        } else if (new File(pd + File.separatorChar + "Makefile").exists()) {
            return TmcProjectType.MAKEFILE;
        } else {
            return TmcProjectType.JAVA_SIMPLE;
        }
    }

    @Deprecated
    public RecursiveZipper.ZippingDecider getZippingDecider() {
        if (getProjectType() == TmcProjectType.JAVA_MAVEN) {
            return new MavenZippingDecider(this);
        } else {
            return new DefaultZippingDecider(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TmcProjectInfo) {
            return this.project.equals(((TmcProjectInfo) obj).project);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return project.hashCode();
    }

    @Deprecated
    private abstract static class AbstractZippingDecider implements RecursiveZipper.ZippingDecider {

        protected TmcProjectInfo projectInfo;

        public AbstractZippingDecider(TmcProjectInfo projectInfo) {
            this.projectInfo = projectInfo;
        }

        protected String withoutRootDir(String zipPath) {
            int i = zipPath.indexOf('/');
            if (i != -1) {
                return zipPath.substring(i + 1);
            } else {
                return "";
            }
        }

        protected boolean isExplicitlyStudentFile(String zipPath) {
            // TODO: make glob patterns like 'foo/bar/*/baz/**/xoox.*' possible
            return projectInfo.getTmcProjectFile().getExtraStudentFiles().contains(withoutRootDir(zipPath));
        }

        protected boolean hasNoSubmitFile(File dir) {
            return new File(dir, ".tmcnosubmit").exists();
        }

        protected abstract boolean isProbablySourceFile(String zipPath);

        @Override
        public boolean shouldZip(String zipPath) {
            File file = new File(projectInfo.getProjectDirAsFile().getParentFile(), zipPath);
            if (isExplicitlyStudentFile(zipPath)) {
                return true;
            }

            if (file.isDirectory()) {
                if (hasNoSubmitFile(file)) {
                    return false;
                }
            }

            return isProbablySourceFile(zipPath);
        }
    }

    @Deprecated
    private static class DefaultZippingDecider extends AbstractZippingDecider {

        public DefaultZippingDecider(TmcProjectInfo projectInfo) {
            super(projectInfo);
        }

        @Override
        protected boolean isProbablySourceFile(String zipPath) {
            return zipPath.contains("/src/");
        }
    }

    @Deprecated
    private static class MavenZippingDecider extends AbstractZippingDecider {

        private static final Pattern rejectPattern = Pattern.compile("^[^/]+/(target|lib/testrunner)/.*");

        public MavenZippingDecider(TmcProjectInfo projectInfo) {
            super(projectInfo);
        }

        @Override
        protected boolean isProbablySourceFile(String zipPath) {
            return !rejectPattern.matcher(zipPath).matches();
        }
    }
}