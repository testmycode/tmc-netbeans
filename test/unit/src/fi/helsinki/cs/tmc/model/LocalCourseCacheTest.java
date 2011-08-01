package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.data.Exercise;
import java.io.IOException;
import org.junit.After;
import java.util.logging.Level;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LocalCourseCacheTest {
    
    private Level oldLogLevel;
    
    private ConfigFile file;
    
    private LocalCourseCache cache;
    
    @Before
    public void setUp() {
        oldLogLevel = LocalCourseCache.logger.getLevel();
        LocalCourseCache.logger.setLevel(Level.OFF);
        
        file = new ConfigFile("LocalCourseCacheTest.json");
        cache = new LocalCourseCache(file);
    }
    
    @After
    public void tearDown() throws IOException {
        file.getFileObject().delete();
        LocalCourseCache.logger.setLevel(oldLogLevel);
    }
    
    @Test
    public void itShouldPersitItsCourseList() throws IOException {
        CourseList courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        courses.get(0).getExercises().add(new Exercise("ex1"));
        
        cache.setAvailableCourses(courses);
        cache = new LocalCourseCache(file);
        
        assertEquals("one", cache.getAvailableCourses().get(0).getName());
        assertEquals("ex1", cache.getAvailableCourses().get(0).getExercises().get(0).getName());
    }
    
    @Test
    public void itShouldPersistTheCurrentCourse() throws IOException {
        CourseList courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        
        cache.setAvailableCourses(courses);
        cache.setCurrentCourseName("one");
        cache = new LocalCourseCache(file);
        
        assertEquals("one", cache.getCurrentCourse().getName());
        assertSame(cache.getAvailableCourses().get(0), cache.getCurrentCourse());
    }
    
    @Test
    public void itShouldBeEmptyWhenFailingToLoadTheLocalCacheFile() throws IOException {
        CourseList courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        cache.setAvailableCourses(courses);
        
        file.writeContents("oops!");
        
        cache = new LocalCourseCache(file);
        assertTrue(cache.getAvailableCourses().isEmpty());
    }
    
    @Test
    public void theCurrentCourseShouldHaveAnIdentityOfOneOfTheAvailableCourses() throws IOException {
        CourseList courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        cache.setAvailableCourses(courses);
        cache.setCurrentCourseName("two");
        
        cache = new LocalCourseCache(file);
        assertSame("current course has the wrong object identity", cache.getAvailableCourses().get(1), cache.getCurrentCourse());
    }
    
    @Test
    public void itCanConvenientlyReturnTheExercisesFromTheCurrentCourse() {
        CourseList courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        cache.setAvailableCourses(courses);
        
        assertNull(cache.getCurrentCourse());
        assertTrue(cache.getCurrentCourseExercises().isEmpty());
        
        cache.setCurrentCourseName("two");
        courses.getCourseByName("two").getExercises().add(new Exercise("ex1"));
        assertEquals("ex1", cache.getCurrentCourseExercises().get(0).getName());
    }
    
    @Test
    public void itCanConvenientlyReturnTAllExercisesFromAllCourses() {
        CourseList courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        cache.setAvailableCourses(courses);
        
        assertTrue(cache.getAllExercises().isEmpty());
        
        courses.getCourseByName("one").getExercises().add(new Exercise("ex"));
        courses.getCourseByName("two").getExercises().add(new Exercise("ex"));
        assertEquals("ex", cache.getAllExercises().get(0).getName());
    }
}
