package fi.helsinki.cs.tmc.data.serialization.cresultparser;

import hy.tmc.core.domain.Exercise;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CTestResultParserTest {

    private ArrayList<CTestCase> oneOfEachTest;

    public CTestResultParserTest() {
    }

    @Before
    public void setUp() {
        oneOfEachTest = new ArrayList<CTestCase>();
        oneOfEachTest.add(new CTestCase("passing", "success", "Passed", null));
        oneOfEachTest.add(new CTestCase("failing", "failure", "This test should've failed", null));
    }

    @Test(expected=IllegalStateException.class)
    public void testParsingWithNoTests() throws Exception {
        CTestResultParser cpar = null;
        File tmp = mkTempFile("test_output", ".xml");
        try {
            cpar = new CTestResultParser(tmp, null, null);
            cpar.parseTestOutput();
        } finally {
            tmp.delete();
        }
        assertTrue(cpar.getTestCaseResults().isEmpty());
    }

    @Test
    public void testParsingWithOneSuccessfulTest() {
        CTestResultParser cpar = null;
        try {
            ArrayList<CTestCase> testCases = new ArrayList<CTestCase>();
            testCases.add(oneOfEachTest.get(0));
            File tmp = constructTestOutput(testCases);
            cpar = new CTestResultParser(tmp, null, Exercise.ValgrindStrategy.NONE);
            cpar.parseTestOutput();
            tmp.delete();

        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestCaseResult> results = cpar.getTestCaseResults();
        assertEquals("There should only be one test result", 1, results.size());
        TestCaseResult result = results.get(0);
        assertTrue("The test should be successful", result.isSuccessful());
        assertEquals("The test should contain the message Passed", "Passed", result.getMessage());
        assertEquals("The name of the test should be \"passing\"", "passing", result.getName());
    }

    @Test
    public void testParsingWithOneFailedTest() {
        CTestResultParser cpar = null;
        try {
            ArrayList<CTestCase> testCases = new ArrayList<CTestCase>();
            testCases.add(oneOfEachTest.get(1));
            File tmp = constructTestOutput(testCases);
            cpar = new CTestResultParser(tmp, null, null);
            cpar.parseTestOutput();
            tmp.delete();

        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestCaseResult> results = cpar.getTestCaseResults();
        assertEquals("There should only be one test result", 1, results.size());
        TestCaseResult result = results.get(0);
        assertFalse("The test should not be successful", result.isSuccessful());
        assertEquals("The test should contain the message: This test should've failed", "This test should've failed", result.getMessage());
        assertEquals("The name of the test should be \"failing\"", "failing", result.getName());
    }

    @Test
    public void testParsingWithOneFailingAndOnePassing() {
        CTestResultParser cpar = null;
        try {
            File tmp = constructTestOutput(oneOfEachTest);
            cpar = new CTestResultParser(tmp, null, null);
            cpar.parseTestOutput();
            tmp.delete();

        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestCaseResult> results = cpar.getTestCaseResults();
        assertEquals("There should be two test results", 2, results.size());
        assertTrue("The first should be passing", results.get(0).isSuccessful());
        assertFalse("The second should be failing", results.get(1).isSuccessful());
    }

    @Test
    public void testParsingWithEmptyValgrindOutput() {
        CTestResultParser cpar = null;
        try {
            ArrayList<CTestCase> testCases = new ArrayList<CTestCase>();
            testCases.add(oneOfEachTest.get(1));
            File ttmp = constructTestOutput(testCases);
            File vtmp = constructNotMemoryFailingValgrindOutput(testCases);
            cpar = new CTestResultParser(ttmp, null, null);
            cpar.parseTestOutput();
            ttmp.delete();
            vtmp.delete();

        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestCaseResult> results = cpar.getTestCaseResults();
        assertEquals("There should only be one test result", 1, results.size());
        TestCaseResult result = results.get(0);
        assertFalse("The test should not be successful", result.isSuccessful());
        assertEquals("The test should contain the message: This test should've failed", "This test should've failed", result.getMessage());
        assertEquals("The name of the test should be \"failing\"", "failing", result.getName());
    }

    @Test
    public void testParsingWithValgrindOutput() {
        CTestResultParser cpar = null;
        try {
            File ttmp = constructTestOutput(oneOfEachTest);
            File vtmp = constructMemoryFailingValgrindOutput();

            cpar = new CTestResultParser(ttmp, vtmp, null);
            cpar.parseTestOutput();
            vtmp.delete();
            ttmp.delete();
        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestCaseResult> results = cpar.getTestCaseResults();
        assertEquals("There should be two test results", 2, results.size());
        assertNotNull("Valgrind errors should go in detailed message",
                results.get(0).getDetailedMessage());
        assertTrue("Valgrind errors should go in detailed message",
                results.get(0).getDetailedMessage().contains("==1== 1"));
        assertNull("Valgrind output should go into detailed message if there were not errors",
                results.get(1).getDetailedMessage());
    }

    @Test
    public void testTestsPassWhenNoMemoryErrors() {
        CTestResultParser cpar = null;
        try {
            File ttmp = constructTestOutput(oneOfEachTest);
            File vtmp = constructNotMemoryFailingValgrindOutput(oneOfEachTest);

            cpar = new CTestResultParser(ttmp, vtmp, null);
            cpar.parseTestOutput();
            vtmp.delete();
            ttmp.delete();
        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestCaseResult> results = cpar.getTestCaseResults();
        assertEquals("There should be two test results", 2, results.size());
        for (TestCaseResult r : results) {
            assertNull("Valgrind output should be empty when there was no error",
                    r.getDetailedMessage());
        }
    }

    public File constructMemoryTestOutput(ArrayList<CTestCase> testCases) throws IOException {
        File tmp = mkTempFile("test_memory", ".txt");
        PrintWriter pw = new PrintWriter(tmp, "UTF-8");
        for (CTestCase t : testCases) {
            pw.println(t.getName() + " " + (t.isCheckedForMemoryLeaks() ? "1" : "0") + " " + t.getMaxBytesAllocated());
        }
        pw.close();
        return tmp;
    }

    public File constructTestOutput(ArrayList<CTestCase> testCases) throws IOException {
        File tmp = mkTempFile("test_output", ".xml");
        PrintWriter pw = new PrintWriter(tmp, "UTF-8");
        pw.println("<?xml version=\"1.0\"?>");
        pw.println("<testsuites xmlns=\"http://check.sourceforge.net/ns\">");
        pw.println("  <datetime>2013-02-14 14:57:08</datetime>");
        pw.println("  <suite>");
        pw.println("    <title>tests</title>");
        for (CTestCase t : testCases) {
            pw.println("    <test result=\"" + t.getResult() + "\">");
            pw.println("      <path>.</path>");
            pw.println("      <fn>test.c:1</fn>");
            pw.println("      <id>" + t.getName() + "</id>");
            pw.println("      <iteration>0</iteration>");
            pw.println("      <description>" + t.getName() + "</description>");
            pw.println("      <message>" + t.getMessage() + "</message>");
            pw.println("    </test>");
        }

        pw.println("  </suite>");
        pw.println("  <duration>0.000000</duration>");
        pw.println("</testsuites>");
        pw.flush();
        pw.close();
        return tmp;
    }

    public File constructNotMemoryFailingValgrindOutput(ArrayList<CTestCase> testCases) throws IOException {
        File tmp = mkTempFile("valgrind", ".log");
        PrintWriter pw = new PrintWriter(tmp);
        pw.println("==" + testCases.size() * 2 + 1 + "== Main process");
        int i = 2;
        for (CTestCase t : testCases) {
            pw.println("==" + i * 2 + "== " + (i - 1));
            pw.println("Some crap that should be ignored");
            pw.println("==" + i * 2 + "== ERROR SUMMARY: 0 errors from 0 contexts");
            pw.println("==" + i * 2 + "== LEAK SUMMARY:");
            pw.println("==" + i * 2 + "==   definitely lost: 0 bytes in 0 blocks");
            i++;
        }
        pw.println("==" + testCases.size() * 2 + 1 + "== Done");

        pw.flush();
        pw.close();
        return tmp;
    }

    public File constructMemoryFailingValgrindOutput() throws IOException {
        File tmp = mkTempFile("valgrind", ".log");
        PrintWriter pw = new PrintWriter(tmp);
        pw.println("==10== Main process");
        pw.println("==1== 1");
        pw.println("Some crap that should be ignore");
        pw.println("==1== ERROR SUMMARY: 1 errors from 1 contexts");
        pw.println("==1== LEAK SUMMARY:");
        pw.println("==1==   definitely lost: 0 bytes in 0 blocks");
        pw.println("==1== HEAP SUMMARY:");
        pw.println("==1==   total heap usage: 624 allocs, 237 frees, 0 bytes allocated");
        pw.println("==2== 2");
        pw.println("Some crap that should be ignore");
        pw.println("==2== ERROR SUMMARY: 0 errors from 0 contexts");
        pw.println("==2== LEAK SUMMARY:");
        pw.println("==2==   definitely lost: 124 bytes in 3 blocks");
        pw.println("==2== HEAP SUMMARY:");
        pw.println("==2==   total heap usage: 624 allocs, 237 frees, 0 bytes allocated");
        pw.println("==3== 3");
        pw.println("Some crap that should be ignore");
        pw.println("==3== ERROR SUMMARY: 0 errors from 0 contexts");
        pw.println("==3== LEAK SUMMARY:");
        pw.println("==3==   definitely lost: 0 bytes in 0 blocks");
        pw.println("==3== HEAP SUMMARY:");
        pw.println("==3==   total heap usage: 624 allocs, 237 frees, 100 bytes allocated");
        pw.println("==10== Done");

        pw.flush();
        pw.close();
        return tmp;
    }

    private File mkTempFile(String prefix, String suffix) throws IOException {
        File tmp = File.createTempFile(prefix, suffix);
        tmp.deleteOnExit();
        return tmp;
    }
}
