/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.data.serialization.cresultparser;

import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.utilities.valrindmemorytest.ValgrindMemoryTester;
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

    private File memoryOptions;
    private File testResults;
    private File valgrindOutput;
    private ArrayList<CTestCase> tests;

    public CTestResultParser(File testResults, File valgrindOutput, File memoryOptions) {
        this.testResults = testResults;
        this.valgrindOutput = valgrindOutput;
        this.memoryOptions = memoryOptions;
        this.tests = new ArrayList<CTestCase>();
    }

    public void parseTestOutput() throws SAXException, IOException, ParserConfigurationException {
        try {
            this.tests = parseTestCases(testResults);
        } catch (Exception e) {
        }
        if (valgrindOutput != null) {
            addValgrindOutput();
            
            if (memoryOptions != null) {
                addMemoryTests();
                ValgrindMemoryTester.analyzeMemory(tests);
            }
        }else addWarningToValgrindOutput();

    }

    public List<CTestCase> getTestCases() {
        return this.tests;
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

    private void addMemoryTests() throws FileNotFoundException {
        HashMap<String, String> memoryInfoByName = new HashMap<String, String>();
        Scanner scanner = new Scanner(memoryOptions, "UTF-8");
        while (scanner.hasNextLine()) {
            String[] split = scanner.nextLine().split(" ");
            memoryInfoByName.put(split[0], split[1] + " " + split[2]);
        }
        scanner.close();

        for (CTestCase t : tests) {
            String str = memoryInfoByName.get(t.getName());
            if (str == null) {
                continue;
            }
            String[] params = str.split(" ");
            int checkLeaks, maxBytes;
            try {
                checkLeaks = Integer.parseInt(params[0]);
                maxBytes = Integer.parseInt(params[1]);
            } catch (Exception e) {
                checkLeaks = 0;
                maxBytes = -1;
            }
            t.setMaxBytesAllocated(maxBytes);
            t.setCheckedForMemoryLeaks(checkLeaks == 1);
        }
    }
    
    private void addWarningToValgrindOutput(){
        String message;
        String platform = System.getProperty("os.name").toLowerCase();
        if (platform.contains("linux")){
            message ="Please install valgrind, for debian based distributions run \"apt-get install valgrind\"";
        }else if (platform.contains("mac")){
            message ="Please install valgrind, for OS X we recommend useing homebrew (http://mxcl.github.com/homebrew/) and brew install valgrind";
        } else if (platform.contains("windows")){
            message ="Windows doesn't support valgrind yet";
        } else 
            message = "no instructions available";
        for (int i = 0; i < tests.size(); i++) {
            tests.get(i).setValgrindTrace("Warning, no valgrind availabe - unable to run local memtests\n"
                    + "Follow instructions below or submit to server for memorytesting:\n"+message);
        }
    }

    private void addValgrindOutput() throws FileNotFoundException {
        Scanner scanner = new Scanner(valgrindOutput, "UTF-8");
        String parentOutput = ""; // Contains total amount of memory used and such things. Useful if we later want to add support for testing memory usage
        String[] outputs = new String[tests.size()];
        int[] pids = new int[tests.size()];
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
                outputs[findIndex(pid, pids)] += "\n" + line;
            }
        }
        scanner.close();

        for (int i = 0; i < outputs.length; i++) {
            tests.get(i).setValgrindTrace(outputs[i]);
        }
    }

    private int findIndex(int pid, int[] pids) {
        for (int i = 0; i < pids.length; i++) {
            if (pids[i] == pid) {
                return i;
            }
            if (pids[i] == 0) {
                pids[i] = pid;
                return i;
            }
        }
        return 0;
    }

    private int parsePID(String line) {
        try {
            return Integer.parseInt(line.split(" ")[0].replaceAll("(==|--)", ""));
        } catch (Exception e) {
            return -1;
        }
    }
}
