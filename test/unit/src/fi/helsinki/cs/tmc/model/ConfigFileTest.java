package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.model.ConfigFile;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import static org.junit.Assert.*;

public class ConfigFileTest {
    
    @Before
    @After
    public void clearStore() throws IOException {
        FileObject dir = FileUtil.getConfigRoot().getFileObject("tmc");
        if (dir != null) {
            dir.delete();
        }
    }
    
    @Test
    public void itShouldAllowConvenientReadsAndWritesOfTheFile() throws IOException {
        ConfigFile sf = new ConfigFile("hello.txt");
        sf.writeContents("Hello");
        sf = new ConfigFile("hello.txt");
        assertEquals("Hello", sf.readContents());
    }
}
