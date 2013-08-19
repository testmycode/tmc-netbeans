package fi.helsinki.cs.tmc.utilities.maven;

import fi.helsinki.cs.tmc.utilities.process.ProcessRunner;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.openide.filesystems.FileObject;
import org.openide.windows.InputOutput;

public class MavenRunBuilder {
    private File projectDir = null;
    private List<String> goals = new ArrayList<String>();
    private Map<String, String> props = new HashMap<String, String>();
    private InputOutput io = null;
    
    public MavenRunBuilder() {
    }
    
    public MavenRunBuilder setProjectDir(File projectDir) {
        this.projectDir = projectDir;
        return this;
    }
    
    public MavenRunBuilder addGoal(String goal) {
        goals.add(goal);
        return this;
    }
    
    public MavenRunBuilder setProperty(String key, String value) {
        props.put(key, value);
        return this;
    }
    
    public MavenRunBuilder setProperties(Map<String, String> props) {
        this.props.putAll(props);
        return this;
    }
    
    public MavenRunBuilder setIO(InputOutput io) {
        this.io = io;
        return this;
    }
    
    public ProcessRunner createProcessRunner() {
        if (projectDir == null) {
            throw new IllegalStateException("Project dir not set");
        }
        if (io == null) {
            throw new IllegalStateException("Maven IO not set");
        }
        
        JavaPlatform platform = JavaPlatform.getDefault(); // Should probably use project's configured platform instead
        FileObject javaExe = platform.findTool("java");
        if (javaExe == null) {
            throw new RuntimeException("Java executable not found");
        }
        
        ClassPath classPath = MavenLibs.getMavenClassPath();
        
        String[] command = buildCommand(javaExe, classPath);
        
        return new ProcessRunner(command, projectDir, io);
    }
    
    private String[] buildCommand(FileObject javaExe, ClassPath classPath) {
        List<String> command = new ArrayList<String>(32);
        
        command.add(javaExe.getPath());
        command.add("-cp");
        command.add(classPath.toString(ClassPath.PathConversionMode.WARN));
        command.add("org.apache.maven.cli.MavenCli");
        for (String key : props.keySet()) {
            command.add("-D" + key + "=" + props.get(key));
        }
        command.addAll(goals);
        
        return command.toArray(new String[command.size()]);
    }
}
