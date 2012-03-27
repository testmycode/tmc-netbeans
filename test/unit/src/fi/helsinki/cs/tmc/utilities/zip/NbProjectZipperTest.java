package fi.helsinki.cs.tmc.utilities.zip;

import java.util.ArrayList;
import java.util.List;
import fi.helsinki.cs.tmc.testing.TempTestDir;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class NbProjectZipperTest {
    private static final String SLASH = File.separator;
    private TempTestDir tempDir;
    private String mainDir;
    
    @Before
    public void setUp() throws IOException {
        tempDir = new TempTestDir();
        
        mainDir = tempDir.getPath() + SLASH + "MyExercise";
        new File(mainDir + SLASH + "src" + SLASH + "subdir").mkdirs();
        new File(mainDir + SLASH + "src" + SLASH + "Included1.txt").createNewFile();
        new File(mainDir + SLASH + "src" + SLASH + "subdir" + SLASH + "Included2.txt").createNewFile();
        
        new File(mainDir + SLASH + "Excluded.txt").createNewFile();
        new File(mainDir + SLASH + "test").mkdir();
        new File(mainDir + SLASH + "test" + SLASH + "Excluded.txt").createNewFile();
    }
    
    @After
    public void tearDown() throws IOException {
        tempDir.destroy();
    }
    
    @Test
    public void itShouldZipUpTheSrcSubdirectoryOfTheGivenDirectory() throws IOException {
        List<String> entries = getZipEntries();
        
        if (!entries.contains("MyExercise/src/Included1.txt")) {
            fail("Expected file not in zip.");
        }
        if (!entries.contains("MyExercise/src/subdir/Included2.txt")) {
            fail("Expected file not in zip.");
        }
        if (entries.contains("MyExercise/Excluded.txt")) {
            fail("File that was supposed to be excluded was found in the zip.");
        }
        if (entries.contains("MyExercise/test/Excluded.txt")) {
            fail("File that was supposed to be excluded was found in the zip.");
        }
    }
    
    @Test
    public void itShouldIncludeTestFilesThatAreSpecifiedInTheProjectFile() throws IOException {
        new File(mainDir + SLASH + "test").mkdir();
        new File(mainDir + SLASH + "test" + SLASH + "IncludedTest.txt").createNewFile();
        
        FileUtils.write(new File(mainDir + SLASH + ".tmcproject.yml"), "extra_student_files:\n  - test/IncludedTest.txt", "UTF-8");
        
        List<String> entries = getZipEntries();
        assertTrue(entries.contains("MyExercise/test/IncludedTest.txt"));
        assertFalse(entries.contains("MyExercise/test/Excluded.txt"));
    }

    private List<String> getZipEntries() throws IOException {
        NbProjectZipper zipper = new NbProjectZipper(new File(mainDir));
        byte[] zipData = zipper.zipProjectSources();
        return zipEntryNames(zipData);
    }
    
    private List<String> zipEntryNames(byte[] zipData) throws IOException {
        ArrayList<String> result = new ArrayList<String>();
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData));
        try {
            ZipEntry zent;
            while ((zent = zis.getNextEntry()) != null) {
                result.add(zent.getName());
            }
        } finally {
            zis.close();
        }
        return result;
    }
}
