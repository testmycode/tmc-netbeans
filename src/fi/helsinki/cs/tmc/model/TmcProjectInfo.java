package fi.helsinki.cs.tmc.model;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.openide.filesystems.FileObject;

/**
 * Carries information about a project used in TMC.
 */
public class TmcProjectInfo {
    private OpenProjects openProjects;
    private Project project;
    
    /*package*/ TmcProjectInfo(OpenProjects openProjects, Project project) {
        this.openProjects = openProjects;
        this.project = project;
    }

    /*package*/ Project getProject() {
        return project;
    }
    
    public FileObject getProjectDir() {
        return project.getProjectDirectory();
    }
    
    public void open() {
        openProjects.open(new Project[] { project }, true, true);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TmcProjectInfo) {
            return this.project.equals((TmcProjectInfo)obj);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return project.hashCode();
    }
}
