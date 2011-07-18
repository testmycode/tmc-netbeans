package fi.helsinki.cs.tmc.data.json;

import fi.helsinki.cs.tmc.utilities.json.parsers.JSONCourseListParser;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONExerciseListParser;
import org.junit.Test;
import static org.junit.Assert.*;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import java.util.GregorianCalendar;

public class JSONExerciseListParserTest {
    /**
     * Test of parseJson method, of class JSONExerciseListParser.
     */
    @Test
    public void testParseJson() throws Exception {
        String json = "[{\"name\": \"test\",\"exercise_file\": \"null\",\"deadline\": \"2011-06-14T01:30:19+03:00\",\"return_address\": \"null\"}]";
        Course course = new Course();
        ExerciseCollection result = JSONExerciseListParser.parseJson(json, course);
        
        Exercise exercise = result.searchExercise("test");
        assertSame(course, exercise.getCourse());
        
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(exercise.getDeadline());
        assertEquals(2011, cal.get(GregorianCalendar.YEAR));
        assertEquals(1, cal.get(GregorianCalendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(GregorianCalendar.MINUTE));
    }

    @Test
    public void emptyJson() {
        ExerciseCollection empty = JSONExerciseListParser.parseJson("[]", new Course());
        assertFalse(empty.iterator().hasNext());
    }

    @Test(expected = Exception.class)
    public void nullThrow() throws Exception {
        JSONCourseListParser.parseJson(null);
    }
}
