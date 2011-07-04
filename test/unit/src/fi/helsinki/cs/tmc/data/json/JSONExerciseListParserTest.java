/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.data.json;

import fi.helsinki.cs.tmc.utilities.json.parsers.JSONCourseListParser;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONExerciseListParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import fi.helsinki.cs.tmc.utilities.json.parsers.jsonorg.JSONException;

/**
 *
 * @author knordman
 */
public class JSONExerciseListParserTest {

    private String json;

    public JSONExerciseListParserTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        json = "[{\"exercise\": {\"name\": \"testi\",\"exercise_file\": \"null\",\"deadline\": \"2011-06-14T01:30:19+03:00\",\"return_address\": \"null\"}}]";
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of parseJson method, of class JSONExerciseListParser.
     */
    @Test
    public void testParseJson() throws Exception {
        System.out.println("parseJson");
        ExerciseCollection result = JSONExerciseListParser.parseJson(json, new Course());
        Exercise exercise = result.searchExercise("testi");
        assertTrue(exercise.getName().equals("testi"));
    }

    @Test
    public void emptyJson() {
        try {
            ExerciseCollection empty = JSONExerciseListParser.parseJson("[]", new Course());
            assertTrue(true);

        } catch (JSONException jex) {
            fail(jex.getMessage());
        } catch (Exception e) {
            fail("Can't process empty JSON!");
        }
    }

    @Test(expected = Exception.class)
    public void nullThrow() throws Exception {
        JSONCourseListParser.parseJson(null);
    }
}
