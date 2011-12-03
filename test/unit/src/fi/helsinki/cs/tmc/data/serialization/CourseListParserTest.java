package fi.helsinki.cs.tmc.data.serialization;

import org.junit.Test;
import static org.junit.Assert.*;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseList;
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
    public void itShouldParseJsonCourseLists() {
        String exercisesJson =
                "[{" +
                "name: \"TheExercise\"," +
                "return_url: \"http://example.com/courses/123/exercises/1/submissions\"," +
                "deadline: \"2011-06-14T01:30:19+03:00\"," +
                "zip_url: \"http://example.com/courses/123/exercises/1.zip\"," +
                "attempted: true," +
                "completed: false," +
                "returnable: true" +
                "}]";
        String json = "{api_version: 1, courses: [{\"name\": \"TheCourse\",\"exercises\": " + exercisesJson + "}]}";
        
        CourseList result = parser.parseFromJson(json);
        
        Course course = result.getCourseByName("TheCourse");
        assertEquals("TheCourse", course.getName());
        
        Exercise exercise = course.getExercises().getExerciseByName("TheExercise");
        
        assertEquals("TheCourse", exercise.getCourseName());
        
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(exercise.getDeadline());
        assertEquals(2011, cal.get(GregorianCalendar.YEAR));
        assertEquals(1, cal.get(GregorianCalendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(GregorianCalendar.MINUTE));
        
        assertEquals("http://example.com/courses/123/exercises/1.zip", exercise.getDownloadUrl());
        assertEquals("http://example.com/courses/123/exercises/1/submissions", exercise.getReturnUrl());
        assertTrue(exercise.isAttempted());
        assertFalse(exercise.isCompleted());
        assertTrue(exercise.isReturnable());
    }
    
    @Test
    public void itShouldParseAnEmptyJsonArrayAsAnEmptyCourseList() {
        CourseList empty = parser.parseFromJson("{api_version: 1, courses: []}");
        assertFalse(empty.iterator().hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void itShouldThrowAnNullPointerExceptionIfTheInputIsEmpty() throws Exception {
        parser.parseFromJson(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void itShouldThrowAnIllegalArgumentExceptionIfTheInputIsEmpty() throws Exception {
        parser.parseFromJson("   ");
    }
    
    @Test
    public void itShouldParseANullDeadlineAsNull() {
        String exercisesJson =
                "[{" +
                "name: \"TheExercise\"," +
                "return_url: \"http://example.com/courses/123/exercises/1/submissions\"," +
                "deadline: \"2011-06-14T01:30:19+03:00\"," +
                "zip_url: \"http://example.com/courses/123/exercises/1.zip\"," +
                "attempted: true," +
                "completed: false," +
                "returnable: true" +
                "}]";
        String json = "{api_version: 1, courses: [{\"name\": \"TheCourse\",\"exercises\": " + exercisesJson + "}]}";
        
        CourseList result = parser.parseFromJson(json);
        
        assertNull(result.get(0).getExercises().get(0).getDeadline());
    }
}
