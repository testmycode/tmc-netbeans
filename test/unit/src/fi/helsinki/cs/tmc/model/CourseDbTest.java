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

public class CourseDbTest {
    
    private Level oldLogLevel;
    
    private ConfigFile file;
    
    private CourseDb db;
    
    @Before
    public void setUp() {
        oldLogLevel = CourseDb.logger.getLevel();
        CourseDb.logger.setLevel(Level.OFF);
        
        file = new ConfigFile("CourseDbTest.json");
        db = new CourseDb(file);
    }
    
    @After
    public void tearDown() throws IOException {
        file.getFileObject().delete();
        CourseDb.logger.setLevel(oldLogLevel);
    }
    
    @Test
    public void itShouldPersitItsCourseList() throws IOException {
        CourseList courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        courses.get(0).getExercises().add(new Exercise("ex1"));
        
        db.setAvailableCourses(courses);
        db = new CourseDb(file);
        
        assertEquals("one", db.getAvailableCourses().get(0).getName());
        assertEquals("ex1", db.getAvailableCourses().get(0).getExercises().get(0).getName());
    }
    
    @Test
    public void itShouldPersistTheCurrentCourse() throws IOException {
        CourseList courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        
        db.setAvailableCourses(courses);
        db.setCurrentCourseName("one");
        db = new CourseDb(file);
        
        assertEquals("one", db.getCurrentCourse().getName());
        assertSame(db.getAvailableCourses().get(0), db.getCurrentCourse());
    }
    
    @Test
    public void itShouldBeEmptyWhenFailingToLoadTheFile() throws IOException {
        CourseList courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        db.setAvailableCourses(courses);
        
        file.writeContents("oops!");
        
        db = new CourseDb(file);
        assertTrue(db.getAvailableCourses().isEmpty());
    }
    
    @Test
    public void theCurrentCourseShouldHaveAnIdentityOfOneOfTheAvailableCourses() throws IOException {
        CourseList courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        db.setAvailableCourses(courses);
        db.setCurrentCourseName("two");
        
        db = new CourseDb(file);
        assertSame("current course has the wrong object identity", db.getAvailableCourses().get(1), db.getCurrentCourse());
    }
    
    @Test
    public void itCanConvenientlyReturnTheExercisesFromTheCurrentCourse() {
        CourseList courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        db.setAvailableCourses(courses);
        
        assertNull(db.getCurrentCourse());
        assertTrue(db.getCurrentCourseExercises().isEmpty());
        
        db.setCurrentCourseName("two");
        courses.getCourseByName("two").getExercises().add(new Exercise("ex1"));
        assertEquals("ex1", db.getCurrentCourseExercises().get(0).getName());
    }
    
    @Test
    public void itCanConvenientlyReturnTAllExercisesFromAllCourses() {
        CourseList courses = new CourseList();
        courses.add(new Course("one"));
        courses.add(new Course("two"));
        db.setAvailableCourses(courses);
        
        assertTrue(db.getAllExercises().isEmpty());
        
        courses.getCourseByName("one").getExercises().add(new Exercise("ex"));
        courses.getCourseByName("two").getExercises().add(new Exercise("ex"));
        assertEquals("ex", db.getAllExercises().get(0).getName());
    }
}
