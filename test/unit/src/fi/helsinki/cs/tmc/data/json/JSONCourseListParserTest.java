package fi.helsinki.cs.tmc.data.json;

import org.junit.Test;
import static org.junit.Assert.*;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;

public class JSONCourseListParserTest {
    @Test
    public void testParseJson() {
        String json = "[{\"name\": \"test\",\"exercises_json\": \"./test.json\"}]";
        CourseCollection result = JSONCourseListParser.parseJson(json);
        
        Course expResult = result.getCourseByName("test");
        assertEquals("test", expResult.getName());
        assertEquals("./test.json", expResult.getExerciseListDownloadAddress());
    }
    
    @Test
    public void emptyJson() {
        CourseCollection empty = JSONCourseListParser.parseJson("[]");
        assertFalse(empty.iterator().hasNext());
    }

    @Test(expected = Exception.class)
    public void nullThrow() throws Exception {
        JSONCourseListParser.parseJson(null);
    }
}
