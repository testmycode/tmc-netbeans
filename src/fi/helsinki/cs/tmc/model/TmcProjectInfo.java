package fi.helsinki.cs.tmc.model;

import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 * Carries information about a project used in TMC.
 */
public class TmcProjectInfo {
    private Project project;
    
    /*package*/ TmcProjectInfo(Project project) {
        this.project = project;
    }

    /*package*/ Project getProject() {
        return project;
    }
    
    public FileObject getProjectDir() {
        return project.getProjectDirectory();
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
