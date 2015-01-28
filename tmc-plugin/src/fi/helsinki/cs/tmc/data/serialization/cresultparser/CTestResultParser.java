package fi.helsinki.cs.tmc.data.serialization.cresultparser;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.Exercise.ValgrindStrategy;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CTestResultParser {
    protected static final Logger log = Logger.getLogger(CTestResultParser.class.getName());

    private File testResults;
    private File valgrindOutput;
    private Exercise.ValgrindStrategy valgrindStrategy;
    private ArrayList<CTestCase> tests;

    public CTestResultParser(File testResults, File valgrindOutput, ValgrindStrategy valgrindStrategy) {
        this.testResults = testResults;
        this.valgrindOutput = valgrindOutput;
        this.valgrindStrategy = valgrindStrategy;
        this.tests = new ArrayList<CTestCase>();
    }

    public void parseTestOutput() throws Exception {
        this.tests = parseTestCases(testResults);
        if (valgrindOutput != null) {
            addValgrindOutput();
        } else {
            addWarningToValgrindOutput();
        }

    }

    public List<CTestCase> getTestCases() {
        return this.tests;
    }

    public List<TestCaseResult> getTestCaseResults() {
        log.log(INFO, "Creating TestCaseResults.");
        List<TestCaseResult> tcaseResults = new ArrayList<TestCaseResult>();
        for (CTestCase test : tests) {
            tcaseResults.add(test.createTestCaseResult());
        }
        log.log(INFO, "Created TestCaseResults.");
        return tcaseResults;
    }


    private ArrayList<CTestCase> parseTestCases(File testOutput) throws ParserConfigurationException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        dBuilder.setErrorHandler(null); // Silence logging
        dbFactory.setValidating(false);
        Document doc = null;

        InputStream inputStream = new FileInputStream(testOutput);
        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");

        try {
            doc = dBuilder.parse(is);
        } catch (SAXException ex) {
            log.info("SAX parser error ocurred");
            log.info(ex.toString());
        }

        if (doc == null) {
            log.log(INFO, "doc cannot be null - can't parse test results :(");
            throw new IllegalStateException("doc cannot be null - can't parse test results :(");
        }

        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("test");
        ArrayList<CTestCase> cases = new ArrayList<CTestCase>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String result = node.getAttribute("result");
            String name = node.getElementsByTagName("description").item(0).getTextContent();
            String message = node.getElementsByTagName("message").item(0).getTextContent();
            cases.add(new CTestCase(name, result, message, valgrindStrategy));
        }
        log.log(INFO, "C testcases parsed.");
        return cases;
    }

    private void addWarningToValgrindOutput() {
        String message;
        String platform = System.getProperty("os.name").toLowerCase();
        if (platform.contains("linux")) {
            message = "Please install valgrind. For Debian-based distributions, run `sudo apt-get install valgrind`.";
        } else if (platform.contains("mac")) {
            message = "Please install valgrind. For OS X we recommend using homebrew (http://mxcl.github.com/homebrew/) and `brew install valgrind`.";
        } else if (platform.contains("windows")) {
            message = "Windows doesn't support valgrind yet.";
        } else {
            message = "Please install valgrind if possible.";
        }
        for (int i = 0; i < tests.size(); i++) {
            tests.get(i).setValgrindTrace(
                    "Warning, valgrind not available - unable to run local memory tests\n"
                    + message
                    + "\nYou may also submit the exercise to the server to have it memory-tested.");
        }
    }

    private void addValgrindOutput() throws FileNotFoundException {
        Scanner scanner = new Scanner(valgrindOutput, "UTF-8");
        String parentOutput = ""; // Contains total amount of memory used and such things. Useful if we later want to add support for testing memory usage
        String[] outputs = new String[tests.size()];
        int[] pids = new int[tests.size()];
        int[] errors = new int[tests.size()];
        for (int i = 0; i < outputs.length; i++) {
            outputs[i] = "";
        }

	Pattern errpat = Pattern.compile("ERROR SUMMARY: ([0-9]+)");

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
                Matcher m = errpat.matcher(line);
                if (m.find()) {
                    errors[findIndex(pid, pids)] = Integer.parseInt(m.group(1));
                }
            }
        }
        scanner.close();

        for (int i = 0; i < outputs.length; i++) {
	    if (errors[i] == 0) {
		outputs[i] = "";
	    }
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
