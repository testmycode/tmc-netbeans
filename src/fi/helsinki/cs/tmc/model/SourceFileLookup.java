package fi.helsinki.cs.tmc.model;

import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.openide.filesystems.FileObject;

/**
 * Looks up the source FileObject of a given fully qualified class name.
 */
public class SourceFileLookup {
    public static SourceFileLookup getDefault() {
        return new SourceFileLookup();
    }

    private SourceFileLookup() {
    }

    public FileObject findSourceFileFor(String className) {
        String outerClassName = className.replaceAll("\\$.*$", "");
        String path = outerClassName.replace('.', '/') + ".java";
        return GlobalPathRegistry.getDefault().findResource(path);
    }
}
