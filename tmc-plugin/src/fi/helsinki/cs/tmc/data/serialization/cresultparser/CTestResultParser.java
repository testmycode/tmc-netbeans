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
    private File valgrindOutput;
    private ArrayList<CTestCase> tests;

    public CTestResultParser(File testResults, File valgrindOutput) {
        this.testResults = testResults;
        this.valgrindOutput = valgrindOutput;
        this.tests = new ArrayList<CTestCase>();
    }

    public void parseTestOutput() throws SAXException, IOException, ParserConfigurationException {
        this.tests = parseTestCases(testResults);
        if (valgrindOutput != null) {
            addValgrindOutput();
        }
    }

    public List<TestCaseResult> getTestCaseResults() {
        List<TestCaseResult> tcaseResults = new ArrayList<TestCaseResult>();
        for (CTestCase test : tests) {
            tcaseResults.add(test.createTestCaseResult());
        }
        return tcaseResults;
    }

    private ArrayList<CTestCase> parseTestCases(File testOutput) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(testOutput);

        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("test");
        ArrayList<CTestCase> tests = new ArrayList<CTestCase>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String result = node.getAttribute("result");
            String name = node.getElementsByTagName("description").item(0).getTextContent();
            String message = node.getElementsByTagName("message").item(0).getTextContent();
            tests.add(new CTestCase(name, result, message));
        }

        return tests;
    }

    private void addValgrindOutput() throws FileNotFoundException {
        Scanner scanner = new Scanner(valgrindOutput, "UTF-8");
        String parentOutput = ""; // Contains total amount of memory used and such things. Useful if we later want to add support for testing memory usage
        String[] outputs = new String[tests.size()];
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
            tests.get(i).setValgrindTrace(outputs[i]);
        }
    }

    private int parsePID(String line) {
        try {
            return Integer.parseInt(line.split(" ")[0].replaceAll("(==|--)", ""));
        } catch (Exception e) {
            return -1;
        }
    }
}
