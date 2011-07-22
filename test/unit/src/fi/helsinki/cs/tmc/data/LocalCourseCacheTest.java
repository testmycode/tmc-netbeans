package fi.helsinki.cs.tmc.data;

import java.io.IOException;
import org.junit.After;
import fi.helsinki.cs.tmc.settings.ConfigFile;
import java.util.logging.Level;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LocalCourseCacheTest {
    
    private ConfigFile file;
    
    @Before
    public void setUp() {
        file = new ConfigFile("LocalCourseCacheTest.json");
    }
    
    @After
    public void tearDown() throws IOException {
        file.getFileObject().delete();
    }
    
    @Test
    public void itShouldPersistItsData() throws IOException {
        LocalCourseCache cache = new LocalCourseCache(file);
        CourseCollection courses = new CourseCollection();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        cache.setAvailableCourses(courses);
        cache = new LocalCourseCache(file);
        assertEquals("one", cache.getAvailableCourses().get(0).getName());
        
        cache.setCurrentCourse(courses.get(0));
        cache = new LocalCourseCache(file);
        assertEquals("one", cache.getCurrentCourse().getName());
        
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
        LocalCourseCache cache = new LocalCourseCache(file);
        CourseCollection courses = new CourseCollection();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        cache.setAvailableCourses(courses);
        
        file.writeContents("oops!");
        
        Level oldLevel = LocalCourseCache.logger.getLevel();
        LocalCourseCache.logger.setLevel(Level.OFF);
        try {
            cache = new LocalCourseCache(file);
            assertTrue(cache.getAvailableCourses().isEmpty());
        } finally {
            LocalCourseCache.logger.setLevel(oldLevel);
        }
    }
}
