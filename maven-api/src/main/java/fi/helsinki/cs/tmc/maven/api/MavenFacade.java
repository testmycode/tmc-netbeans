package fi.helsinki.cs.tmc.maven.api;

import fi.helsinki.cs.tmc.maven.impl.MavenTaskRunner;
import java.io.File;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.RequestProcessor;
import org.openide.windows.InputOutput;

/**
 * A simple API to run Maven tasks in NetBeans.
 */
public class MavenFacade {
    private static final RequestProcessor requestProcessor = new RequestProcessor("Maven tasks", 1, true);
    
    public static Future<Integer> runMavenTask(File projectDir, String goal, Map<String, String> props, InputOutput io) {
        return runMavenTask(projectDir, new String[] { goal }, props, io);
    }
    
    public static Future<Integer> runMavenTask(File projectDir, String[] goals, Map<String, String> props, InputOutput io) {
        io.closeInputOutput();
        return runMavenTask(projectDir, goals, props, io.getOut(), io.getErr());
    }
    
    public static Future<Integer> runMavenTask(File projectDir, String goal, Map<String, String> props, Writer stdout, Writer stderr) {
        return runMavenTask(projectDir, new String[] { goal }, props, stdout, stderr);
    }
    
    public static Future<Integer> runMavenTask(File projectDir, String[] goals, Map<String, String> props, Writer stdout, Writer stderr) {
        final MavenTaskRunner mtr = new MavenTaskRunner(projectDir, goals, props, stdout, stderr);
        
        String title = "Maven task: " + goals;
        final ProgressHandle progress = ProgressHandleFactory.createHandle(title);
        progress.start();
        
        return requestProcessor.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                mtr.run();
                progress.finish();
                return mtr.getExitCode();
            }
        });
    }
}
