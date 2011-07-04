/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package palikka.utilities.zip;

import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
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
public class ZipperTest {
    
    public ZipperTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        
        try {
            FileWriter fileWriter;

            (new File("test_dir")).mkdir();
            (new File("test_dir/src")).mkdir();
            
            fileWriter = new FileWriter("test_dir/src/ziptest.txt");
            for (int i = 0; i < 1000000; i++) {
                Long n = (Long) Math.round(Math.random() * 255.0);
                fileWriter.write(n.toString() + "\n");
            }

            fileWriter.close();

        } catch (IOException exception) {
            
        }
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of zip method, of class Zipper.
     */
    @Test
    public void testZip_String() throws Exception {
        System.out.println("zip");
        String path = "test_dir";
        Zipper zipper = new Zipper();
        byte[] zipResult = zipper.zip(path);
        assertNotNull(zipResult);
        
        System.out.println("unzip");
        Unzipper unzipper = new Unzipper();
        String result = unzipper.unZip(new ByteArrayInputStream(zipResult), "test_dir_2");
        assertEquals(result, "test_dir/");
    }

    /**
     * Test of zip method, of class Zipper.
     */
    @Test
    public void testZip_File() throws Exception {
        System.out.println("zip");
        File file = new File("test_dir");
        Zipper instance = new Zipper();
        byte[] result = instance.zip(file);
        assertNotNull(result);
        
        (new File("test_dir_3")).mkdir();
        FileOutputStream streamOut = new FileOutputStream("test_dir_3/test_data.zip");
        
        streamOut.write(result);
        streamOut.flush();
        streamOut.close();
        
        System.out.println("unzip");
        Unzipper unzipper = new Unzipper();
        String result2 = unzipper.unZip(new File("test_dir_3/test_data.zip"), "test_dir_3");
        assertEquals(result2, "test_dir/");
    }
}
