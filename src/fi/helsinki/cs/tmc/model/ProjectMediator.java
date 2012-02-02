package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.Exercise;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.LifecycleManager;
import org.openide.filesystems.FileObject;
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
        return new TmcProjectInfo(p);
    }
    
    public String getProjectRootDir() {
        return TmcSettings.getDefault().getProjectRootDir();
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
                ex.getName().replaceAll("/", "-");
        return new File(path);
    }
    
    /**
     * Returns the exercise associated with the given project, or null if none.
     */
    public Exercise tryGetExerciseForProject(TmcProjectInfo project, CourseDb courseDb) {
        File projectDir = FileUtil.toFile(project.getProjectDir());
        for (Exercise ex : courseDb.getAllExercises()) {
            if (getProjectDirForExercise(ex).equals(projectDir)) {
                return ex;
            }
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
    
    public boolean isProjectOpen(TmcProjectInfo project) {
        return openProjects.isProjectOpen(project.getProject());
    }
    
}
