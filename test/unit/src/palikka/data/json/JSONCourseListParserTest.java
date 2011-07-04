/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package palikka.data.json;

import palikka.utilities.json.parsers.JSONCourseListParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import palikka.data.Course;
import palikka.data.CourseCollection;
import palikka.utilities.json.parsers.jsonorg.JSONException;

/**
 *
 * @author knordman
 */
public class JSONCourseListParserTest {

    private String json;
    
    public JSONCourseListParserTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        json = "[ {\"course\": {\"name\": \"test\",\"exercises_json\": \"./test.json\"}}]";
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of parseJson method, of class JSONCourseListParser.
     */
    @Test
    public void testParseJson() {
        try {
        System.out.println("parseJson");
        CourseCollection result = JSONCourseListParser.parseJson(json);
        
        Course expResult = result.searchCourse("test");
        assertTrue(expResult.getName().equals("test") && expResult.getExerciseListDownloadAddress().equals("./test.json"));
        
        } catch(JSONException jex) {
            fail("Failed to parse JSON!");
        }
    }
    
    @Test
    public void emptyJson() {
        try {
        CourseCollection empty = JSONCourseListParser.parseJson("[]");
        assertTrue(true);
        
        } catch(JSONException jex) {
            fail(jex.getMessage());
        } catch(Exception e) {
            fail("Can't process empty JSON!");
        }
    }

    @Test(expected = Exception.class)
    public void nullThrow() throws Exception {
        JSONCourseListParser.parseJson(null);
    }
}
