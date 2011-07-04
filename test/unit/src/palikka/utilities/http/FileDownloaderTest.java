/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package palikka.utilities.http;

import java.io.InputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ttkoivis
 */
public class FileDownloaderTest {
    
    public FileDownloaderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getFileContent method, of class FileDownloader.
     */
    @Test
    public void testGetFileContent() {
        System.out.println("getFileContent");
        try {
            FileDownloader instance = new FileDownloader("http://");
            instance.setTimeout(100);
            InputStream result = instance.getFileContent();
            fail("Exception was expected");
        }
        catch(Exception exception) {
            assertTrue(true);
        }
        
    }

    /**
     * Test of download method, of class FileDownloader.
     */
    @Test(expected = Exception.class)
    public void testDownload() throws Exception {
        System.out.println("download");
        FileDownloader instance = new FileDownloader("http://");
        instance.download();
    }
}
