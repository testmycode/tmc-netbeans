package fi.helsinki.cs.tmc.utilities.zip;

import fi.helsinki.cs.tmc.utilities.zip.NbProjectUnzipper.Result;
import fi.helsinki.cs.tmc.testing.TempTestDir;
import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NbProjectUnzipperTest {
    
    private final String fsep = File.separator;
    
    private TempTestDir tempDir;
    private ByteArrayOutputStream zipBuffer;
    private ZipOutputStream zipOut;
    
    private NbProjectUnzipper unzipper;
    
    @Before
    public void setUp() throws IOException {
        tempDir = new TempTestDir();
        
        zipBuffer = new ByteArrayOutputStream();
        zipOut = new ZipOutputStream(zipBuffer);
        
        unzipper = new NbProjectUnzipper();
    }
    
    @After
    public void tearDown() throws IOException {
        tempDir.destroy();
    }
    
    private void addFakeProjectToZip(String path, String name) throws IOException {
        writeDirToZip(path + "/");
        writeDirToZip(path + "/nbproject/");
        writeFileToZip(path + "/nbproject/project.xml", "Fake project.xml of " + name);
        writeDirToZip(path + "/src/");
        writeFileToZip(path + "/src/Hello.java", "Fake Java file of " + name);
    }
    
    private void writeDirToZip(String path) throws IOException {
        assertTrue(path.endsWith("/"));
        zipOut.putNextEntry(new ZipEntry(path));
    }

    private void writeFileToZip(String path, String content) throws IOException {
        ZipEntry zent = new ZipEntry(path);
        zipOut.putNextEntry(zent);
        Writer w = new OutputStreamWriter(zipOut, "UTF-8");
        w.write(content);
        w.flush();
    }
    
    private File inTempDir(String subpath) {
        return new File(tempDir.getPath() + File.separator + subpath);
    }
    
    @Test
    public void itShouldUnzipTheFirstProjectDirectoryItSeesInAZip() throws IOException {
        addFakeProjectToZip("dir1/dir12/project1", "P1");
        addFakeProjectToZip("dir2/project2", "P2");
        addFakeProjectToZip("project3", "P3");
        zipOut.close();
        
        Result result = unzipper.unzipProject(zipBuffer.toByteArray(), inTempDir("my-project"), "my-project");
        
        assertEquals(1, tempDir.get().listFiles().length);
        String contents = FileUtils.readFileToString(new File(tempDir.getPath() + File.separator + "my-project/nbproject/project.xml"));
        assertEquals("Fake project.xml of P1", contents);
        contents = FileUtils.readFileToString(new File(tempDir.getPath() + File.separator + "my-project/src/Hello.java"));
        assertEquals("Fake Java file of P1", contents);
        
        assertTrue(result.newFiles.contains("nbproject" + fsep + "project.xml"));
        assertTrue(result.newFiles.contains("src" + fsep + "Hello.java"));
        assertEquals(2, result.newFiles.size());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void itShouldFailIfTheZipContainsNoProjectDirectory() throws IOException {
        writeDirToZip("dir1/");
        writeDirToZip("dir1/dir2/");
        writeFileToZip("dir1/dir2/oops.txt", "oops");
        zipOut.close();
        
        unzipper.unzipProject(zipBuffer.toByteArray(), inTempDir("my-project"), "my-project");
    }
    
    @Test
    public void itShouldOnlyOverwriteExistingFilesIfTheyveChangedAndTheOverwritingDeciderPermits() throws IOException {
        writeDirToZip("dir1/");
        writeDirToZip("dir1/nbproject/");
        writeFileToZip("dir1/one.txt", "one");
        writeFileToZip("dir1/two.txt", "two");
        writeFileToZip("dir1/three.txt", "three");
        writeFileToZip("dir1/four.txt", "four");
        zipOut.close();
        
        new File(tempDir.getPath() + "/dest").mkdirs();
        File expectedPreservedFile = new File(tempDir.getPath() + "/dest/one.txt");
        FileUtils.write(expectedPreservedFile, "This should remain");
        File expectedOverwrittenFile = new File(tempDir.getPath() + "/dest/two.txt");
        FileUtils.write(expectedOverwrittenFile, "This should be overwritten");
        File expectedNewFile = new File(tempDir.getPath() + "/dest/three.txt");
        File expectedSameFile = new File(tempDir.getPath() + "/dest/four.txt");
        FileUtils.write(expectedSameFile, "four");
        
        NbProjectUnzipper.OverwritingDecider overwriting = mock(NbProjectUnzipper.OverwritingDecider.class);
        when(overwriting.canOverwrite("one.txt")).thenReturn(false);
        when(overwriting.canOverwrite("two.txt")).thenReturn(true);
        
        Result result = unzipper.unzipProject(zipBuffer.toByteArray(), inTempDir("dest"), "dest", overwriting);
        
        verify(overwriting).canOverwrite("one.txt");
        verify(overwriting).canOverwrite("two.txt");
        verifyNoMoreInteractions(overwriting); // Should only call for existing files
        
        assertEquals("This should remain", FileUtils.readFileToString(expectedPreservedFile));
        assertEquals("two", FileUtils.readFileToString(expectedOverwrittenFile));
        assertEquals("three", FileUtils.readFileToString(expectedNewFile));
        assertEquals("four", FileUtils.readFileToString(expectedSameFile));
        
        assertTrue(result.skippedFiles.contains("one.txt"));
        assertTrue(result.overwrittenFiles.contains("two.txt"));
        assertTrue(result.newFiles.contains("three.txt"));
        assertTrue(result.unchangedFiles.contains("four.txt"));
        assertEquals(1, result.newFiles.size());
        assertEquals(1, result.overwrittenFiles.size());
        assertEquals(1, result.skippedFiles.size());
        assertEquals(1, result.unchangedFiles.size());
    }
    
    @Test
    public void itCanWorkInDryRunMode() throws IOException {
        writeDirToZip("dir1/");
        writeDirToZip("dir1/nbproject/");
        writeFileToZip("dir1/one.txt", "one");
        writeFileToZip("dir1/two.txt", "two");
        zipOut.close();
        
        new File(tempDir.getPath() + "/dest").mkdirs();
        File file1 = new File(tempDir.getPath() + "/dest/one.txt");
        FileUtils.write(file1, "This should remain");
        File file2 = new File(tempDir.getPath() + "/dest/two.txt");
        FileUtils.write(file2, "This would be overwritten if not in dry run mode");
        
        NbProjectUnzipper.OverwritingDecider overwriting = mock(NbProjectUnzipper.OverwritingDecider.class);
        when(overwriting.canOverwrite("one.txt")).thenReturn(false);
        when(overwriting.canOverwrite("two.txt")).thenReturn(true);
        
        Result result = unzipper.unzipProject(zipBuffer.toByteArray(), inTempDir("dest"), "dest", overwriting, false);
        
        verify(overwriting).canOverwrite("one.txt");
        verify(overwriting).canOverwrite("two.txt");
        
        assertEquals("This should remain", FileUtils.readFileToString(file1));
        assertEquals("This would be overwritten if not in dry run mode", FileUtils.readFileToString(file2));
        
        assertTrue(result.skippedFiles.contains("one.txt"));
        assertTrue(result.overwrittenFiles.contains("two.txt"));
        assertEquals(1, result.overwrittenFiles.size());
        assertEquals(1, result.skippedFiles.size());
    }
}
