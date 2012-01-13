package fi.helsinki.cs.tmc.data.serialization;

import java.util.List;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.testrunner.CaughtException;
import static fi.helsinki.cs.tmc.data.SubmissionResult.Status.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SubmissionResultParserTest {
    
    private SubmissionResult parse(String json) {
        return new SubmissionResultParser().parseFromJson(json);
    }
    
    @Test
    public void testOk() {
        String input = "{status: \"ok\", solution_url: \"http://example.com/solution\"}";
        
        SubmissionResult result = parse(input);
        
        assertEquals(OK, result.getStatus());
        assertEquals("http://example.com/solution", result.getSolutionUrl());
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
    
    @Test
    public void testExceptions() {
        String traceJson = "[{declaringClass: \"Foo\", methodName: \"bar\", fileName: \"Foo.java\", lineNumber: 123}]";
        String exceptionJson = "{className: \"FooEx\", message: \"xoo\", stackTrace: " + traceJson + ", cause: null}";
        String testCasesJson = "[{name: \"A test\", successful: false, message: \"it failed\", exception: " + exceptionJson + "}]";
        String input = "{status: \"fail\", test_cases: " + testCasesJson + "}";
        
        SubmissionResult result = parse(input);
        
        CaughtException cex = result.getTestCases().get(0).getException();
        assertNotNull(cex);
        assertEquals("FooEx", cex.className);
        assertEquals("xoo", cex.message);
        assertNull(null, cex.cause);
        
        StackTraceElement[] trace = cex.stackTrace;
        assertNotNull(trace);
        assertEquals("Foo", trace[0].getClassName());
        assertEquals("bar", trace[0].getMethodName());
        assertEquals("Foo.java", trace[0].getFileName());
        assertEquals(123, trace[0].getLineNumber());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void itShouldThrowAnIllegalArgumentExceptionWhenGivenAnEmptyInput() {
        parse("   ");
    }
}
