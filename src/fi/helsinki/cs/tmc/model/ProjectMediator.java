package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.Exercise;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.LifecycleManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbPreferences;

/**
 * The sole interface to query and update the NetBeans project list from TMC.
 */
public class ProjectMediator {
    // This is a difficult thing to test because the NetBeans Project API
    // is so very unmockable.
    
    private static final Logger logger = Logger.getLogger(ProjectMediator.class.getName());
    
    private static final String PREF_PROJECT_ROOT_DIR = "projectRootDir";
    
    private static ProjectMediator instance;

    public static ProjectMediator getInstance() {
        if (instance == null) {
            instance = new ProjectMediator();
        }
        return instance;
    }
    
    private static Preferences getPreferences() {
        return NbPreferences.forModule(ServerAccess.class);
    }

    
    private OpenProjects openProjects;
    private ProjectManager projectManager;
    
    public ProjectMediator() {
        this.openProjects = OpenProjects.getDefault();
        this.projectManager = ProjectManager.getDefault();
    }
    
    public String getProjectRootDir() {
        String dir = getPreferences().get(PREF_PROJECT_ROOT_DIR, null);
        if (dir != null) {
            return dir;
        } else {
            return getDefaultProjectRootDir();
        }
    }
    
    private String getDefaultProjectRootDir() {
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
    
    private File getDirectoryContainingMainProject() {
        Project project = openProjects.getMainProject();
        if (project != null) {
            return new File(project.getProjectDirectory().getParent().getPath());
        } else {
            return null;
        }
    }

    public void setProjectRootDir(String projectDir) {
        getPreferences().put(PREF_PROJECT_ROOT_DIR, projectDir);
    }
    
    /**
     * Returns the directory of the main project, or null if no main project selected.
     */
    public TmcProjectInfo getMainProject() {
        Project project = openProjects.getMainProject();
        if (project != null) {
            return wrapProject(project);
        } else {
            return null;
        }
    }
    
    /**
     * Returns the list of projects that are open right now.
     */
    public List<TmcProjectInfo> getOpenProjects() {
        // todo: should this use openProjects().get() instead?
        Project[] projects = openProjects.getOpenProjects();
        TmcProjectInfo[] infos = new TmcProjectInfo[projects.length];
        for (int i = 0; i < projects.length; ++i) {
            infos[i] = wrapProject(projects[i]);
        }
        return Arrays.asList(infos);
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
    public Exercise tryGetExerciseForProject(TmcProjectInfo project, LocalCourseCache courseCache) {
        File projectDir = FileUtil.toFile(project.getProjectDir());
        for (Exercise ex : courseCache.getAvailableExercises()) {
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
    
    private TmcProjectInfo wrapProject(Project p) {
        return new TmcProjectInfo(openProjects, p);
    }
    
}
