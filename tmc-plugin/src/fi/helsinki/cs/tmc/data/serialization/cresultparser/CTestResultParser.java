/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.data.serialization.cresultparser;

import fi.helsinki.cs.tmc.data.TestCaseResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author rase
 */
public class CTestResultParser {

    private File testResults;
    private File testPoints;
    private File valgrindOutput;
    private HashMap<String, CTestCase> tests;
    private HashMap<String, CTestSuite> testSuites;

    public CTestResultParser(File testResults, File testPoints, File valgrindOutput) {
        this.testResults = testResults;
        this.testPoints = testPoints;
        this.valgrindOutput = valgrindOutput;
        this.tests = new HashMap<String, CTestCase>();
        this.testSuites = new HashMap<String, CTestSuite>();
    }

    public void parseTestOutput() throws SAXException, IOException, ParserConfigurationException {
        this.testSuites = parseTestSuites(testResults);
        this.tests = parseTestCases(testResults);
        if (testPoints != null) {
            addPoints();
        }
        if (valgrindOutput != null) {
            addValgrindOutput();
        }
    }

    public List<TestCaseResult> getTestCaseResults() {
        List<TestCaseResult> tcaseResults = new ArrayList<TestCaseResult>();
        for (CTestCase test : tests.values()) {
            tcaseResults.add(test.createTestCaseResult());
        }
        return tcaseResults;
    }

    private HashMap<String, CTestSuite> parseTestSuites(File testOutput) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(testOutput);

        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("suite");
        HashMap<String, CTestSuite> suites = new HashMap<String, CTestSuite>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String name = node.getElementsByTagName("title").item(0).getTextContent();
            suites.put(name, new CTestSuite(name));
        }
        return suites;
    }

    private HashMap<String, CTestCase> parseTestCases(File testOutput) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(testOutput);

        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("test");
        HashMap<String, CTestCase> tests = new HashMap<String, CTestCase>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String result = node.getAttribute("result");
            String name = node.getElementsByTagName("description").item(0).getTextContent();
            String message = node.getElementsByTagName("message").item(0).getTextContent();
            tests.put(name, new CTestCase(name, result, message));
        }

        return tests;
    }

    private void addPoints() throws FileNotFoundException {
        Scanner scanner = new Scanner(testPoints, "UTF-8");
        while (scanner.hasNextLine()) {
            String[] splitLine = scanner.nextLine().split(" ");
            if (splitLine[0].equals("[test]")) {
                String name = splitLine[1];
                String points = join(splitLine, " ", 2);
                CTestCase associatedTest = tests.get(name);
                if (associatedTest != null) {
                    associatedTest.setPoints(points);
                }
            } else if (splitLine[0].equals("[suite]")) {
                String name = splitLine[1];
                String points = join(splitLine, " ", 2);
                CTestSuite associatedSuite = testSuites.get(name);
                if (associatedSuite != null) {
                    associatedSuite.setPoints(points);
                }
            } else {
                // Do nothing at least of for now
            }

        }
        scanner.close();
    }

    private void addValgrindOutput() throws FileNotFoundException {
        ArrayList<CTestCase> testList = new ArrayList<CTestCase>(tests.values());
        Scanner scanner = new Scanner(valgrindOutput, "UTF-8");
        String parentOutput = ""; // Contains total amount of memory used and such things. Useful if we later want to add support for testing memory usage
        String[] outputs = new String[testList.size()];
        for (int i = 0; i < outputs.length; i++) {
            outputs[i] = "";
        }

        String line = scanner.nextLine();
        int firstPID = parsePID(line);
        parentOutput += "\n" + line;
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            int pid = parsePID(line);
            if (pid == -1) {
                continue;
            }
            if (pid == firstPID) {
                parentOutput += "\n" + line;
            } else {
                int index = pid - firstPID - 1;
                outputs[index] += "\n" + line;
            }
        }
        scanner.close();

        for (int i = 0; i < outputs.length; i++) {
            testList.get(i).setValgrindTrace(outputs[i]);
        }
    }

    private String join(String r[], String d, int i) {
        StringBuilder sb = new StringBuilder();
        for (; i < r.length - 1; i++) {
            sb.append(r[i] + d);
        }
        return sb.toString() + r[i];
    }

    private int parsePID(String line) {
        try {
            return Integer.parseInt(line.split(" ")[0].replaceAll("(==|--)", ""));
        } catch (Exception e) {
            return -1;
        }
    }
}
