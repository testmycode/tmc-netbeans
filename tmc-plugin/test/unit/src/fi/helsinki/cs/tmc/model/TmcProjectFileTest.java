package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.testing.TempTestDir;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TmcProjectFileTest {
    private TempTestDir tempDir;
    
    @Before
    public void setUp() throws IOException {
        tempDir = new TempTestDir();
    }
    
    @After
    public void tearDown() throws IOException {
        tempDir.destroy();
    }
    
    @Test
    public void testLoading() throws IOException {
        writeFile("extra_student_files:\n  - \"one\"\n  - \"two\"");
        TmcProjectFile result = TmcProjectFile.load(getFile());
        assertTrue(result.getExtraStudentFiles().contains("one"));
        assertTrue(result.getExtraStudentFiles().contains("two"));
        assertEquals(2, result.getExtraStudentFiles().size());
    }
    
    @Test
    public void testLoadingEmptyFile() throws IOException {
        writeFile("---");
        TmcProjectFile result = TmcProjectFile.load(getFile());
        assertTrue(result.getExtraStudentFiles().isEmpty());
    }

    private File getFile() {
        return new File(tempDir.get().getPath() + File.separator + ".tmcproject.yml");
    }
    
    private void writeFile(String content) throws IOException {
        File file = getFile();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.println(content);
        writer.close();
    }
}
