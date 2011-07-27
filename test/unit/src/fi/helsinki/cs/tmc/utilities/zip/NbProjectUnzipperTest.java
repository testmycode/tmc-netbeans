package fi.helsinki.cs.tmc.utilities.zip;

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

public class NbProjectUnzipperTest {
    
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
    
    @Test
    public void itShouldUnzipTheFirstProjectDirectoryItSeesInAZip() throws IOException {
        addFakeProjectToZip("dir1/dir12/project1", "P1");
        addFakeProjectToZip("dir2/project2", "P2");
        addFakeProjectToZip("project3", "P3");
        zipOut.close();
        
        unzipper.unzipProject(zipBuffer.toByteArray(), tempDir.get(), "my-project");
        
        assertEquals(1, tempDir.get().listFiles().length);
        String contents = FileUtils.readFileToString(new File(tempDir.getPath() + File.separator + "my-project/nbproject/project.xml"));
        assertEquals("Fake project.xml of P1", contents);
        contents = FileUtils.readFileToString(new File(tempDir.getPath() + File.separator + "my-project/src/Hello.java"));
        assertEquals("Fake Java file of P1", contents);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void itShouldFailIfTheZipContainsNoProjectDirectory() throws IOException {
        writeDirToZip("dir1/");
        writeDirToZip("dir1/dir2/");
        writeFileToZip("dir1/dir2/oops.txt", "oops");
        zipOut.close();
        
        unzipper.unzipProject(zipBuffer.toByteArray(), tempDir.get(), "my-project");
    }
    
    @Test
    public void itShouldNeverNeverNeverEverOverwriteExistingFiles() throws IOException {
        addFakeProjectToZip("dir1/dir12/project1", "P1");
        zipOut.close();
        
        new File(tempDir.getPath() + "/my-project/src").mkdirs();
        File existingFile = new File(tempDir.getPath() + "/my-project/src/Hello.java");
        FileUtils.write(existingFile, "This should remain");
        
        boolean caughtException = false;
        try {
            unzipper.unzipProject(zipBuffer.toByteArray(), tempDir.get(), "my-project");
        } catch (IllegalStateException e) {
            caughtException = true;
        }
        assertTrue(caughtException);
        
        assertEquals("This should remain", FileUtils.readFileToString(existingFile));
    }
}
