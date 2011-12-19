package fi.helsinki.cs.tmc.data.serialization;

import java.util.List;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.data.TestCaseResult;
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
        assertTrue(result.getTestCases().isEmpty());
    }
    
    @Test
    public void testError() {
        String input = "{status: \"error\", error: \"Failed to compile.\"}";
        
        SubmissionResult result = parse(input);
        
        assertEquals(ERROR, result.getStatus());
        assertEquals("Failed to compile.", result.getError());
        assertTrue(result.getTestCases().isEmpty());
    }
    
    @Test
    public void testFail() {
        String testCasesJson = "[{name: \"Some test\", successful: true}, {name: \"Another test\", successful: false, message: \"it failed\"}]";
        String input = "{status: \"fail\", test_cases: " + testCasesJson + "}";
        
        SubmissionResult result = parse(input);
        
        assertEquals(FAIL, result.getStatus());
        assertNull(result.getError());
        
        List<TestCaseResult> testCases = result.getTestCases();
        assertEquals(2, testCases.size());
        assertEquals("Some test", testCases.get(0).getName());
        assertEquals("Another test", testCases.get(1).getName());
        assertTrue(testCases.get(0).isSuccessful());
        assertFalse(testCases.get(1).isSuccessful());
        assertNull(testCases.get(0).getMessage());
        assertEquals("it failed", testCases.get(1).getMessage());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void itShouldThrowAnIllegalArgumentExceptionWhenGivenAnEmptyInput() {
        parse("   ");
    }
}
