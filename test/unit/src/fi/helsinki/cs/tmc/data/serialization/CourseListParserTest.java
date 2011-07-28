package fi.helsinki.cs.tmc.data.serialization;

import org.junit.Test;
import static org.junit.Assert.*;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.data.Exercise;
import java.util.GregorianCalendar;
import org.junit.Before;

public class CourseListParserTest {
    
    private CourseListParser parser;
    
    @Before
    public void setUp() {
        parser = new CourseListParser();
    }
    
    @Test
    public void testParseJson() {
        String exercisesJson =
                "[{" +
                "name: \"TheExercise\"," +
                "return_address: \"http://example.com/courses/123/exercises/1/submissions\"," +
                "deadline: \"2011-06-14T01:30:19+03:00\"," +
                "publish_date: null," +
                "zip_url: \"http://example.com/courses/123/exercises/1.zip\"," +
                "attempted: true," +
                "completed: false" +
                "}]";
        String json = "[{\"name\": \"TheCourse\",\"exercises\": " + exercisesJson + "}]";
        CourseCollection result = parser.parseFromJson(json);
        
        Course course = result.getCourseByName("TheCourse");
        assertEquals("TheCourse", course.getName());
        
        Exercise exercise = course.getExercises().getExerciseByName("TheExercise");
        
        assertEquals("TheCourse", exercise.getCourseName());
        
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
        CourseCollection empty = parser.parseFromJson("[]");
        assertFalse(empty.iterator().hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void nullThrow() throws Exception {
        parser.parseFromJson(null);
    }
}
