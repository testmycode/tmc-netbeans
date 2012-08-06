package fi.helsinki.cs.tmc.utilities.zip;

import java.util.ArrayList;
import java.util.List;
import fi.helsinki.cs.tmc.testing.TempTestDir;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecursiveZipperTest {
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
        new File(mainDir + SLASH + "test").mkdir();
        new File(mainDir + SLASH + "excluded").mkdir();
        new File(mainDir + SLASH + "excluded" + SLASH + "Foo.txt").createNewFile();
    }
    
    @After
    public void tearDown() throws IOException {
        tempDir.destroy();
    }
    
    @Test
    public void itShouldZipRecursivelyFilesThatZipDeciderSelects() throws IOException {
        RecursiveZipper.ZippingDecider decider = new RecursiveZipper.ZippingDecider() {
            @Override
            public boolean shouldZip(String relativeZipPath) {
                return !relativeZipPath.equals("MyExercise/Excluded.txt") &&
                        !relativeZipPath.equals("MyExercise/src/Excluded.txt") &&
                        !relativeZipPath.equals("MyExercise/excluded/");
            }
        };
        List<String> entries = getZipEntries(decider);
        
        if (!entries.contains("MyExercise/src/Included1.txt")) {
            fail("Expected file not in zip.");
        }
        if (!entries.contains("MyExercise/src/subdir/Included2.txt")) {
            fail("Expected file not in zip.");
        }
        if (entries.contains("MyExercise/Excluded.txt")) {
            fail("File that was supposed to be excluded was found in the zip.");
        }
        if (entries.contains("MyExercise/src/Excluded.txt")) {
            fail("File that was supposed to be excluded was found in the zip.");
        }
        if (entries.contains("MyExercise/excluded/Foo.txt")) {
            fail("File that was supposed to be excluded was found in the zip.");
        }
    }

    private List<String> getZipEntries(RecursiveZipper.ZippingDecider decider) throws IOException {
        RecursiveZipper zipper = new RecursiveZipper(new File(mainDir), decider);
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
