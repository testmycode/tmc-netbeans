package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.testing.TempTestDir;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExerciseUpdateOverwritingDeciderTest {
    private static final String SLASH = File.separator;
    private TempTestDir tempDir;
    private String mainDir;
    private ExerciseUpdateOverwritingDecider decider;
    
    @Before
    public void setUp() throws IOException {
        tempDir = new TempTestDir();
        
        mainDir = tempDir.getPath() + SLASH + "MyExercise";
        new File(mainDir + SLASH + "src" + SLASH + "subdir").mkdirs();
        new File(mainDir + SLASH + "src" + SLASH + "SrcFile1.txt").createNewFile();
        new File(mainDir + SLASH + "src" + SLASH + "subdir" + SLASH + "SrcFile2.txt").createNewFile();
        
        new File(mainDir + SLASH + "RootFile.txt").createNewFile();
        
        new File(mainDir + SLASH + "test").mkdir();
        new File(mainDir + SLASH + "test" + SLASH + "TestFile.txt").createNewFile();
        new File(mainDir + SLASH + "test" + SLASH + "IncludedByProjectFile.txt").createNewFile();
        
        FileUtils.write(new File(mainDir + SLASH + ".tmcproject.yml"), "extra_student_files:\n  - test/IncludedByProjectFile.txt", "UTF-8");
        
        decider = new ExerciseUpdateOverwritingDecider(new File(mainDir));
    }
    
    @Test
    public void testOverwritingAndDeleting() {
        assertFalse(decider.mayOverwrite("src" + SLASH + "SrcFile1.txt"));
        assertFalse(decider.mayOverwrite("src" + SLASH + "subdir" + SLASH + "SrcFile2.txt"));
        assertFalse(decider.mayDelete("src" + SLASH + "SrcFile1.txt"));
        assertFalse(decider.mayDelete("src" + SLASH + "subdir" + SLASH + "SrcFile2.txt"));
        
        assertTrue(decider.mayOverwrite("RootFile.txt"));
        assertFalse(decider.mayDelete("RootFile.txt"));
        
        assertTrue(decider.mayOverwrite("test" + SLASH + "TestFile.txt"));
        assertTrue(decider.mayDelete("test" + SLASH + "TestFile.txt"));
        
        assertFalse(decider.mayOverwrite("test" + SLASH + "IncludedByProjectFile.txt"));
        assertFalse(decider.mayDelete("test" + SLASH + "IncludedByProjectFile.txt"));
        
        assertTrue(decider.mayOverwrite(".tmcproject.yml"));
        assertTrue(decider.mayDelete(".tmcproject.yml"));
    }
    
    @After
    public void tearDown() throws IOException {
        tempDir.destroy();
    }
}
