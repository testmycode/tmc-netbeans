/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.data.serialization.cresultparser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author rase
 */
public class CTestResultParserTest {
    private ArrayList<CTestCase> oneOfEachTest;
    
    public CTestResultParserTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        oneOfEachTest = new ArrayList<CTestCase>();
        oneOfEachTest.add(new CTestCase("passing", "success", "Passed"));
        oneOfEachTest.add(new CTestCase("failing", "failure", "This test should've failed"));
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testParsingWithNoTests() {
        CTestResultParser cpar = null;
        try {
            File tmp = File.createTempFile("test_output", ".xml");
            cpar = new CTestResultParser(tmp, null);
            cpar.parseTestOutput();
            tmp.delete();
        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
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
            cpar = new CTestResultParser(tmp, null);
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
            cpar = new CTestResultParser(tmp, null);
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
            cpar = new CTestResultParser(tmp, null);
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
            File vtmp = constructValgrindOutput(testCases);
            cpar = new CTestResultParser(ttmp, null);
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
            File vtmp = constructValgrindOutput(oneOfEachTest);
            
            cpar = new CTestResultParser(ttmp, vtmp);
            cpar.parseTestOutput();
            vtmp.delete();
            ttmp.delete();
        } catch (Exception e) {
            fail("Error creating or parsing mock output file: " + e.getMessage());
        }
        List<TestCaseResult> results = cpar.getTestCaseResults();
        assertEquals("There should be two test results", 2, results.size());
        int i = 2;
        for (TestCaseResult r : results) {
            assertEquals("\n==" + i * 2 + "== " + (i - 1), r.getValgrindTrace());
            i++;
        }
    }
    
    public File constructTestOutput(ArrayList<CTestCase> testCases) throws IOException {
        File tmp = File.createTempFile("test_output", ".xml");
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
    
    public File constructValgrindOutput(ArrayList<CTestCase> testCases) throws IOException {
        File tmp = File.createTempFile("valgrind", ".log");
        PrintWriter pw = new PrintWriter(tmp);
        pw.println("==" + testCases.size() * 2 + 1 +"== Main process");
        int i = 2;
        for (CTestCase t : testCases) {
            pw.println("==" + i * 2 + "== " + (i - 1));
            pw.println("Some crap that should be ignore");
            i++;
        }
        pw.println("==" + testCases.size() * 2 + 1 +"== Done");
        
        pw.flush();
        pw.close();
        return tmp;
    }
}
