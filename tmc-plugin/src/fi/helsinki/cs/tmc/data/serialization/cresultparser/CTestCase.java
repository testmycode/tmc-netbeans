/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.data.serialization.cresultparser;

import fi.helsinki.cs.tmc.data.TestCaseResult;

/**
 *
 * @author rase
 */
public class CTestCase {
    private String name;
    private String result;
    private String message;
    private String points;
    private String valgrindTrace;

    public CTestCase(String name, String result, String message, String points, String valgrindTrace) {
        this(name);
        this.result = result;
        this.message = message;
        this.points = points;
        this.valgrindTrace = valgrindTrace;
    }

    public CTestCase(String name, String result, String message) {
        this(name, result, message, null, null);
    }

    public CTestCase(String name) {
        this.name = name;
    }
    
    public TestCaseResult createTestCaseResult() {
        boolean successful = (result.equals("success"));
        return new TestCaseResult(name, successful, message + valgrindTrace);
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPoints() {
        return this.points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    public String getValgrindTrace() {
        return valgrindTrace;
    }

    public void setValgrindTrace(String valgrindTrace) {
        this.valgrindTrace = valgrindTrace;
    }
}
