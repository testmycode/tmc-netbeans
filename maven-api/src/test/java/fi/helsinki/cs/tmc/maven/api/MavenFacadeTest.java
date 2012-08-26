package fi.helsinki.cs.tmc.maven.api;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import static org.junit.Assert.*;
import org.junit.Test;

public class MavenFacadeTest {
    private static final File testProjectDir = new File(new File(".").getAbsolutePath() + File.separator + "test-project");
    
    @Test
    public void testRunningMavenGoal() throws Exception {
        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();
        final String[] goals = new String[] { "clean", "test" };
        Map<String, String> props = new HashMap<String, String>();
        
        Future<Integer> testFuture = MavenFacade.runMavenTask(testProjectDir, goals, props, stdout, stderr);
        
        assertEquals(0, (int)testFuture.get());
        assertTrue(stdout.toString().contains("BUILD SUCCESS"));
        assertTrue(stdout.toString().contains("Hello from TestProject trivial test case."));
    }
}
