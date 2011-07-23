package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
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
    public void itShouldPersitItsData() throws IOException {
        CourseCollection courses = new CourseCollection();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        cache.setAvailableCourses(courses);
        cache = new LocalCourseCache(file);
        assertEquals("one", cache.getAvailableCourses().get(0).getName());
        
        cache.setCurrentCourseName("one");
        cache = new LocalCourseCache(file);
        assertEquals("one", cache.getCurrentCourse().getName());
        assertSame(cache.getAvailableCourses().get(0), cache.getCurrentCourse());
        
        ExerciseCollection exercises = new ExerciseCollection();
        exercises.add(new Exercise("Hello"));
        exercises.add(new Exercise("Hello2"));
        cache.setAvailableExercises(exercises);
        cache = new LocalCourseCache(file);
        assertEquals("Hello2", cache.getAvailableExercises().get(1).getName());
        
        assertFalse(file.readContents().isEmpty());
    }
    
    @Test
    public void itShouldBeEmptyWhenFailingToLoadTheLocalCacheFile() throws IOException {
        CourseCollection courses = new CourseCollection();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        cache.setAvailableCourses(courses);
        
        file.writeContents("oops!");
        
        cache = new LocalCourseCache(file);
        assertTrue(cache.getAvailableCourses().isEmpty());
    }
    
    @Test
    public void theCurrentCourseShouldHaveAnIdentityOfOneOfTheAvailableCourses() throws IOException {
        CourseCollection courses = new CourseCollection();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        cache.setAvailableCourses(courses);
        cache.setCurrentCourseName("two");
        
        cache = new LocalCourseCache(file);
        assertSame("current course has the wrong object identity", cache.getAvailableCourses().get(1), cache.getCurrentCourse());
    }
    
    @Test
    public void whenTheCurrentCourseIsChangedTheListOfAvailableExercisesShouldBeCleared() {
        CourseCollection courses = new CourseCollection();
        courses.add(new Course("CourseOne"));
        courses.add(new Course("CourseTwo"));
        cache.setAvailableCourses(courses);
        
        ExerciseCollection oldExercises = new ExerciseCollection();
        oldExercises.add(new Exercise("ExOne"));
        cache.setAvailableExercises(oldExercises);
        cache.setCurrentCourseName("CourseTwo");
        
        assertTrue(cache.getAvailableExercises().isEmpty());
    }
}
