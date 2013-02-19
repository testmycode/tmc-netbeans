
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


class TestSuite {
    private String name;
    private String points;
    
    public TestSuite(String name) {
        this.name = name;
    }
    
    public TestSuite(String name, String points) {
        this(name);
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }
    
}

class Test {
    private String name;
    private String result;
    private String message;
    private String points;
    private String valgrindTrace;

    public Test(String name, String result, String message, String points, String valgrindTrace) {
        this(name);
        this.result = result;
        this.message = message;
        this.points = points;
        this.valgrindTrace = valgrindTrace;
    }

    public Test(String name, String result, String message) {
        this(name, result, message, null, null);
    }

    public Test(String name) {
        this.name = name;
    }

    public String serialize() {
        return "";
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
/**
 * 
 * @author rase
 */
public class Parse {

    public static void main(String[] args) {
        String cmd = "make; valgrind ./test/test";
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(cmd);
        pr.waitFor();
        
        File testOutput = new File("tmc_test_results.xml");
        File testPoints = new File("tmc_available_points.txt");
        File valgrindTraces = new File("val.log");

        try {
            HashMap<String, Test> tests = parseTests(testOutput);
            HashMap<String, TestSuite> testSuites = parseTestSuites(testOutput);
            addPoints(testPoints, tests, testSuites);
            addValgrindOutput(valgrindTraces, new ArrayList<Test>(tests.values()));
            System.out.println(tests.get("test_bar").getValgrindTrace());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static HashMap<String, TestSuite> parseTestSuites(File testOutput) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(testOutput);

        doc.getDocumentElement().normalize();
        
        NodeList nodeList = doc.getElementsByTagName("suite");
        HashMap<String, TestSuite> suites = new HashMap<String, TestSuite>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String name = node.getElementsByTagName("title").item(0).getTextContent();
            suites.put(name, new TestSuite(name));
        }
        return suites;
    }
    
    private static HashMap<String, Test> parseTests(File testOutput) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(testOutput);

        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("test");
        HashMap<String, Test> tests = new HashMap<String, Test>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String result = node.getAttribute("result");
            String name = node.getElementsByTagName("description").item(0).getTextContent();
            String message = node.getElementsByTagName("message").item(0).getTextContent();
            tests.put(name, new Test(name, result, message));
        }

        return tests;
    }

    private static void addPoints(File testPoints, HashMap<String, Test> tests, HashMap<String, TestSuite> testSuites) throws FileNotFoundException {
        Scanner scanner = new Scanner(testPoints, "UTF-8");
        while (scanner.hasNextLine()) {
            String[] splitLine = scanner.nextLine().split(" ");
            if (splitLine[0].equals("[test]")) {
                String name = splitLine[1];
                String points = join(splitLine, " ", 2);
                Test associatedTest = tests.get(name);
                if (associatedTest != null) {
                    associatedTest.setPoints(points);
                }
            } else if (splitLine[0].equals("[suite]")) {
                String name = splitLine[1];
                String points = join(splitLine, " ", 2);
                TestSuite associatedSuite = testSuites.get(name);
                if (associatedSuite != null) {
                    associatedSuite.setPoints(points);
                }
            } else {
                // Do nothing at least of for now
            }

        }
        scanner.close();
    }

    private static void addValgrindOutput(File valgrindOutput, ArrayList<Test> tests) throws FileNotFoundException {
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
            if (pid == -1) continue;
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

    private static int parsePID(String line) {
        try {
            return Integer.parseInt(line.split(" ")[0].replaceAll("(==|--)", ""));
        } catch (Exception e) {
            return -1;
        }
    }
    
    public static String join(String r[],String d, int i) {
        StringBuilder sb = new StringBuilder();
        for(;i<r.length-1;i++)
            sb.append(r[i]+d);
        return sb.toString()+r[i];
    }
}