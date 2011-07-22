package fi.helsinki.cs.tmc.model;

import java.io.File;
import java.util.prefs.Preferences;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.util.NbPreferences;

/**
 * The sole interface to query and update the NetBeans project list from TMC.
 */
public class ProjectMediator {
    // This is a difficult thing to test because the NetBeans Project API
    // is so very unmockable.
    
    private static final String PREF_PROJECT_DIR = "projectDir";
    
    private static ProjectMediator instance;

    public static ProjectMediator getInstance() {
        if (instance == null) {
            instance = new ProjectMediator();
        }
        return instance;
    }
    
    private static Preferences getPreferences() {
        return NbPreferences.forModule(TmcServerAccess.class);
    }

    
    private OpenProjects openProjects;
    
    public ProjectMediator() {
        this.openProjects = OpenProjects.getDefault();
    }
    
    public String getProjectDir() {
        String dir = getPreferences().get(PREF_PROJECT_DIR, null);
        if (dir != null) {
            return dir;
        } else {
            return getDefaultProjectDir();
        }
    }
    
    private String getDefaultProjectDir() {
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

    public void setProjectDir(String projectDir) {
        getPreferences().put(PREF_PROJECT_DIR, projectDir);
    }
}
