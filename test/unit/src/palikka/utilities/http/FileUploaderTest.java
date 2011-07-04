/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package palikka.utilities.http;

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
public class FileUploaderTest {
    
    public FileUploaderTest() {
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
     * Test of send method, of class FileUploader.
     */
    @Test(expected = Exception.class)
    public void testSend() throws Exception {
        System.out.println("send");
        FileUploader instance = new FileUploader("http://");
        instance.send();
    }

}
