package fi.helsinki.cs.tmc.data.serialization;

import fi.helsinki.cs.tmc.data.SubmissionResult;
import static fi.helsinki.cs.tmc.data.SubmissionResult.Status.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SubmissionResultParserTest {
    
    private SubmissionResult parse(String json) {
        return new SubmissionResultParser().parseFromJson(json);
    }
    
    @Test
    public void testOk() {
        String input = "{status: \"ok\"}";
        
        SubmissionResult result = parse(input);
        
        assertEquals(OK, result.getStatus());
        assertNull(result.getError());
        assertTrue(result.getCategorizedTestFailures().isEmpty());
    }
    
    @Test
    public void testError() {
        String input = "{status: \"error\", error: \"Failed to compile.\"}";
        
        SubmissionResult result = parse(input);
        
        assertEquals(ERROR, result.getStatus());
        assertEquals("Failed to compile.", result.getError());
        assertTrue(result.getCategorizedTestFailures().isEmpty());
    }
    
    @Test
    public void testFail() {
        String input = "{status: \"fail\", categorized_test_failures: {\"Cat1\": [\"one\", \"two\"], \"Cat2\": [\"three\"]}}";
        
        SubmissionResult result = parse(input);
        
        assertEquals(FAIL, result.getStatus());
        assertNull(result.getError());
        assertEquals(2, result.getCategorizedTestFailures().size());
        assertEquals(2, result.getCategorizedTestFailures().get("Cat1").size());
        assertEquals(1, result.getCategorizedTestFailures().get("Cat2").size());
        
        assertEquals("one", result.getCategorizedTestFailures().get("Cat1").get(0));
        assertEquals("two", result.getCategorizedTestFailures().get("Cat1").get(1));
        assertEquals("three", result.getCategorizedTestFailures().get("Cat2").get(0));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void itShouldThrowAnIllegalArgumentExceptionWhenGivenAnEmptyInput() {
        parse("   ");
    }
}
