package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.Exercise;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.openide.filesystems.FileObject;

/**
 * Looks up the source FileObject of a given fully qualified class name.
 */
public class SourceFileLookup {
    public static SourceFileLookup getDefault() {
        return new SourceFileLookup(ProjectMediator.getInstance(), GlobalPathRegistry.getDefault());
    }

    private final ProjectMediator projectMediator;
    private final GlobalPathRegistry globalPathRegistry;

    private SourceFileLookup(ProjectMediator projectMediator, GlobalPathRegistry globalPathRegistry) {
        this.projectMediator = projectMediator;
        this.globalPathRegistry = globalPathRegistry;
    }

    public FileObject findSourceFileFor(Exercise exercise, String className) {
        String outerClassName = className.replaceAll("\\$.*$", "");
        String path = outerClassName.replace('.', '/') + ".java";

        TmcProjectInfo correctProject = projectMediator.tryGetProjectForExercise(exercise);
        for (FileObject sr : globalPathRegistry.getSourceRoots()) {
            TmcProjectInfo p = projectMediator.tryGetProjectOwningFile(sr);
            if (p != null && p.equals(correctProject)) {
                FileObject result = sr.getFileObject(path);
                if (result != null) {
                    return result;
                }
            }
        }

        // Fall back to findResource picking a source root from any project.
        return GlobalPathRegistry.getDefault().findResource(path);
    }
}
