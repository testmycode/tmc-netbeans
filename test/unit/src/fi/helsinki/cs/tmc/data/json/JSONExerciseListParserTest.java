package fi.helsinki.cs.tmc.data.json;

import org.junit.Test;
import static org.junit.Assert.*;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import java.util.GregorianCalendar;

public class JSONExerciseListParserTest {
    @Test
    public void testParseJson() throws Exception {
        String json =
                "[{" +
                "name: \"test\"," +
                "return_address: \"http://example.com/courses/123/exercises/1/submissions\"," +
                "deadline: \"2011-06-14T01:30:19+03:00\"," +
                "publish_date: null," +
                "zip_url: \"http://example.com/courses/123/exercises/1.zip\"," +
                "attempted: true," +
                "completed: false" +
                "}]";
        ExerciseCollection result = JSONExerciseListParser.parseJson(json);
        
        Exercise exercise = result.getExerciseByName("test");
        
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(exercise.getDeadline());
        assertEquals(2011, cal.get(GregorianCalendar.YEAR));
        assertEquals(1, cal.get(GregorianCalendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(GregorianCalendar.MINUTE));
        
        assertEquals("http://example.com/courses/123/exercises/1.zip", exercise.getDownloadAddress());
        assertEquals("http://example.com/courses/123/exercises/1/submissions", exercise.getReturnAddress());
        assertTrue(exercise.isAttempted());
        assertFalse(exercise.isCompleted());
    }

    @Test
    public void emptyJson() {
        ExerciseCollection empty = JSONExerciseListParser.parseJson("[]");
        assertFalse(empty.iterator().hasNext());
    }

    @Test(expected = Exception.class)
    public void nullThrow() throws Exception {
        JSONCourseListParser.parseJson(null);
    }
}
