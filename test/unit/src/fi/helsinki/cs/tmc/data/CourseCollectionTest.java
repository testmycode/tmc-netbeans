/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.data;

import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.data.Course;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ttkoivis
 */
public class CourseCollectionTest {
    
    private CourseCollection courses;
    
    public CourseCollectionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        Course course;
        
        courses = new CourseCollection();
        for(int i=0; i < 1000; i++) {
            course = new Course();
            course.setName("Course " + i);
            courses.add(course);
        }
            
        
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of searchCourse method, of class CourseCollection.
     */
    @Test
    public void testSearchCourse() {
        System.out.println("searchCourse");
        Course result;
        result = courses.searchCourse(null);
        assertNull(result);
        result = courses.searchCourse("Course 100");
        assertEquals("Course 100", result.getName());
        result = courses.searchCourse("Course 200");
        assertEquals("Course 200", result.getName());
        result = courses.searchCourse("Course 300");
        assertEquals("Course 300", result.getName());
        result = courses.searchCourse("Course 400");
        assertEquals("Course 400", result.getName());
        result = courses.searchCourse("Course 500");
        assertEquals("Course 500", result.getName());
        result = courses.searchCourse("Course 600");
        assertEquals("Course 600", result.getName());
        result = courses.searchCourse("Course 700");
        assertEquals("Course 700", result.getName());
        result = courses.searchCourse("Course 800");
        assertEquals("Course 800", result.getName());
        result = courses.searchCourse("Course 900");
        assertEquals("Course 900", result.getName());
        result = courses.searchCourse("Course 1000");
        assertNull(result);
    }
}
