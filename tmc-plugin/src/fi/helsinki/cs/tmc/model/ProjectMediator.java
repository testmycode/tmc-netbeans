package fi.helsinki.cs.tmc.model;

import hy.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.LifecycleManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

/**
 * The sole interface to query and update the NetBeans project list from TMC.
 */
public class ProjectMediator {
    // This is a difficult thing to test because the NetBeans Project API
    // is so very unmockable.
    
    private static final Logger logger = Logger.getLogger(ProjectMediator.class.getName());
    
    private static ProjectMediator instance;

    public static ProjectMediator getInstance() {
        if (instance == null) {
            instance = new ProjectMediator();
        }
        return instance;
    }
    
    private OpenProjects openProjects;
    private ProjectManager projectManager;
    
    public ProjectMediator() {
        this.openProjects = OpenProjects.getDefault();
        this.projectManager = ProjectManager.getDefault();
    }
    
    public TmcProjectInfo wrapProject(Project p) {
        if (p == null) {
            throw new NullPointerException();
        }
        return new TmcProjectInfo(p);
    }
    
    public List<TmcProjectInfo> wrapProjects(List<Project> projects) {
        List<TmcProjectInfo> result = new ArrayList<TmcProjectInfo>();
        for (Project p : projects) {
            result.add(wrapProject(p));
        }
        return result;
    }
    
    public String getProjectRootDir() {
        return NBTmcSettings.getDefault().getTmcMainDirectory();
    }
    
    public static String getDefaultProjectRootDir() {
        File candidate;
        
        candidate = ProjectChooser.getProjectsFolder();
        if (candidate.isDirectory()) {
            return candidate.getAbsolutePath();
        }
        
        candidate = getDirectoryContainingMainProject();
        if (candidate.isDirectory()) {
            return candidate.getAbsolutePath();
        }
        
        return new File(System.getProperty("user.home") + File.separator + "NetBeansProjects").getAbsolutePath();
    }
    
    private static File getDirectoryContainingMainProject() {
        Project project = OpenProjects.getDefault().getMainProject();
        if (project != null) {
            return new File(project.getProjectDirectory().getParent().getPath());
        } else {
            return null;
        }
    }
    
    /**
     * Saves all unsaved files.
     */
    public void saveAllFiles() {
        LifecycleManager.getDefault().saveAll();
    }
    
    /**
     * Returns the directory to which exercises are to be downloaded.
     */
    public File getCourseRootDir(String courseName) {
        return new File(getProjectRootDir() + File.separator + courseName);
    }
    
    /**
     * Returns the intended project directory of an exercise.
     * 
     * <p>
     * The exercise must have a course name set.
     */
    public File getProjectDirForExercise(Exercise ex) {
        String path = 
                getProjectRootDir() + File.separator +
                ex.getCourseName() + File.separator +
                ex.getName().replaceAll("-", "/");
        File file = new File(path);
        return FileUtil.normalizeFile(file);
    }
    
    /**
     * Returns the exercise associated with the given project, or null if none.
     */
    public Exercise tryGetExerciseForProject(TmcProjectInfo project, CourseDb courseDb) {
        File projectDir = FileUtil.toFile(project.getProjectDir());
        for (Exercise ex : courseDb.getCurrentCourseExercises()) {
            if (getProjectDirForExercise(ex).equals(projectDir)) {
                return ex;
            }
        }
        return null;
    }
    
    /**
     * Attempts to find the project owning the given file object.
     */
    public TmcProjectInfo tryGetProjectOwningFile(FileObject fo) {
        while (fo != null) {
            if (fo.isFolder()) {
                try {
                    Project proj = ProjectManager.getDefault().findProject(fo);
                    if (proj != null) {
                        return wrapProject(proj);
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error finding project owning file: " + fo, ex);
                }
            }
            fo = fo.getParent();
        }
        return null;
    }
    
    /**
     * Returns the project for the exercise, or null if not yet created.
     * 
     * <p>
     * The exercise must have a course name set.
     */
    public TmcProjectInfo tryGetProjectForExercise(Exercise exercise) {
        projectManager.clearNonProjectCache(); // Just to be sure.
        
        File path = getProjectDirForExercise(exercise);
        FileObject fo = FileUtil.toFileObject(path);
        if (fo != null) {
            try {
                Project project = projectManager.findProject(fo);
                if (project != null) {
                    return wrapProject(project);
                } else {
                    return null;
                }
            } catch (IOException ioe) {
                logger.log(
                        Level.WARNING,
                        "Finding project for exercise {0} failed",
                        new Object[] { exercise.toString(), ioe }
                        );
                return null;
            }
        } else {
            return null;
        }
    }
    
    public void openProject(TmcProjectInfo project) {
        openProjects.open(new Project[] { project.getProject() }, true, true);
    }
    
    public void openProjects(Collection<TmcProjectInfo> projects) {
        final Project[] nbProjects = new Project[projects.size()];
        int i = 0;
        for (TmcProjectInfo projectInfo : projects) {
            nbProjects[i++] = projectInfo.getProject();
        }
        
        new Thread("Project opener") {
            @Override
            public void run() {
                openProjects.open(nbProjects, true, true);
            }
        }.start();
    }
    
    public Collection<TmcProjectInfo> getOpenProjects() {
        Project[] projects = openProjects.getOpenProjects();
        return wrapProjects(Arrays.asList(projects));
    }
    
    /**
     * Waits for projects to be fully opened and then calls the given runnable in the EDT.
     */
    public void callWhenProjectsCompletelyOpened(final Runnable whenOpen) {
        Thread waitingThread = new Thread("callWhenProjectsCompletelyOpened() thread") {
            @Override
            public void run() {
                try {
                    openProjects.openProjects().get();
                } catch (Exception ex) {
                    throw ExceptionUtils.toRuntimeException(ex);
                }
                SwingUtilities.invokeLater(whenOpen);
            }
        };
        waitingThread.setDaemon(true);
        waitingThread.start();
    }
    
    public boolean isProjectOpen(TmcProjectInfo project) {
        return openProjects.isProjectOpen(project.getProject());
    }

    /**
     * Refreshes NB's file cache like "Source -> Scan for External Changes".
     */
    public void scanForExternalChanges(TmcProjectInfo project) {
        try {
            project.getProjectDir().getFileSystem().refresh(true);
        } catch (Exception ex) {
        }
    }
    
    /**
     * Refreshes NB's file cache like "Source -> Scan for External Changes".
     */
    public void scanForExternalChanges(Collection<TmcProjectInfo> projects) {
        HashSet<FileSystem> filesystems = new HashSet<FileSystem>();
        for (TmcProjectInfo project : projects) {
            try {
                filesystems.add(project.getProjectDir().getFileSystem());
            } catch (Exception e) {
            }
        }
        for (FileSystem fs : filesystems) { // Probably just one
            fs.refresh(true);
        }
    }
}
