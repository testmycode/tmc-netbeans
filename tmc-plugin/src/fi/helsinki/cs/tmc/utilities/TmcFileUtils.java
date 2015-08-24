package fi.helsinki.cs.tmc.utilities;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;

import org.openide.filesystems.FileObject;

public class TmcFileUtils {
    /**
     * If the file object is owned by a project, returns the path of the file
     * relative to the project.
     *
     * Otherwise returns null.
     */
    public static String tryGetPathRelativeToProject(FileObject fileObject) {
        String filePath = fileObject.getPath();

        try {
            Project p = FileOwnerQuery.getOwner(fileObject);
            String projectDirectory = p.getProjectDirectory().getPath();
            if (filePath.contains(projectDirectory)) {
                filePath = filePath.substring(filePath.indexOf(projectDirectory) + projectDirectory.length());
            }
        } catch (Exception e) {
            return null;
        }

        return filePath;
    }
}