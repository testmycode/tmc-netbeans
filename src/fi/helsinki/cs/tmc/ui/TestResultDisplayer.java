package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringEscapeUtils;
import org.openide.NotifyDescriptor;

public class TestResultDisplayer {
    private static TestResultDisplayer instance;
    
    public static TestResultDisplayer getInstance() {
        if (instance == null) {
            instance = new TestResultDisplayer();
        }
        return instance;
    }
    
    private ConvenientDialogDisplayer dialogs;
    
    /*package*/ TestResultDisplayer() {
        this.dialogs = ConvenientDialogDisplayer.getDefault();
    }
    
    public void showSubmissionResult(SubmissionResult result) {
        switch (result.getStatus()) {
            case OK:
                dialogs.displayHappyMessage("All tests passed!", "Yay!");
                break;
            case FAIL:
                displayTestCases(result.getTestCases());
                break;
            case ERROR:
                displayError(result.getError());
                break;
        }
    }
    
    /**
     * Shows local results and returns whether a submission should be started.
     */
    public boolean showLocalRunResult(List<TestCaseResult> results) {
        int numFailed = 0;
        for (TestCaseResult result : results) {
            if (!result.isSuccessful()) {
                numFailed += 1;
            }
        }
        
        if (numFailed == 0) {
            return dialogs.askYesNo("All tests passed. Submit to server?", "Submit?");
        } else {
            displayTestCases(results);
            return false;
        }
    }
    
    private void displayError(String error) {
        String htmlError =
                "<html><font face=\"monospaced\" color=\"red\">" +
                preformattedToHtml(error) +
                "</font></html>";
        LongTextDisplayPanel panel = new LongTextDisplayPanel(htmlError);
        dialogs.showDialog(panel, NotifyDescriptor.ERROR_MESSAGE, "", false);
    }
    
    private String preformattedToHtml(String text) {
        // <font> doesn't work around pre, so we do this
        return StringEscapeUtils.escapeHtml4(text)
                .replace(" ", "&nbsp;")
                .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                .replace("\n", "<br>");
    }
    
    private void displayTestCases(List<TestCaseResult> testCases) {
        LongTextDisplayPanel panel = new LongTextDisplayPanel(testCasesToHtml(groupTestCases(testCases)));
        dialogs.showDialog(panel, NotifyDescriptor.ERROR_MESSAGE, "Test results", false);
    }
    
    private String testCasesToHtml(Map<String, List<TestCaseResult>> testCases) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<p>Some tests failed.</p>");
        sb.append("<ul>");
        
        String[] groups = testCases.keySet().toArray(new String[0]);
        Arrays.sort(groups);
        for (String group : groups) {
            String groupColor = allSuccessful(testCases.get(group)) ? "green" : "red";
            sb.append("<li>")
                    .append("<font color=\"" + groupColor + "\">")
                    .append(StringEscapeUtils.escapeHtml4(group))
                    .append("</font>")
                    .append("<ul>");
            
            for (TestCaseResult tcr : testCases.get(group)) {
                String color = tcr.isSuccessful() ? "green" : "red";
                sb.append("<li><font color=\"").append(color).append("\">")
                        .append(testCaseLine(tcr))
                        .append("</font></li>");
            }
            
            sb.append("</ul>")
                    .append("</li>");
        }
        sb.append("</ul>").append("</html>");
        return sb.toString();
    }
    
    private Map<String, List<TestCaseResult>> groupTestCases(List<TestCaseResult> testCases) {
        Map<String, List<TestCaseResult>> result = new HashMap<String, List<TestCaseResult>>();
        for (TestCaseResult tcr : testCases) {
            String[] parts = tcr.getName().split(" ", 2);
            String group = parts[0];
            if (!result.containsKey(group)) {
                result.put(group, new ArrayList<TestCaseResult>());
            }
            result.get(group).add(tcr);
        }
        return result;
    }
    
    private String testCaseLine(TestCaseResult tcr) {
        String msg;
        if (tcr.getMessage() != null) {
            msg = StringEscapeUtils.escapeHtml4(tcr.getMessage());
        } else if (tcr.isSuccessful()) {
            msg = "OK";
        } else {
            msg = "Fail";
        }
        return testCaseLinePrefix(tcr) + msg;
    }
    
    private String testCaseLinePrefix(TestCaseResult tcr) {
        String[] parts = tcr.getName().split(" ", 2);
        if (parts.length == 2) {
            return parts[1] + " &ndash; ";
        } else {
            return "";
        }
    }

    private boolean allSuccessful(List<TestCaseResult> testCases) {
        for (TestCaseResult tcr : testCases) {
            if (!tcr.isSuccessful()) {
                return false;
            }
        }
        return true;
    }
}
