package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.SubmissionResult;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringEscapeUtils;
import org.openide.NotifyDescriptor;

public class SubmissionResultDisplayer {
    private static SubmissionResultDisplayer instance;
    
    public static SubmissionResultDisplayer getInstance() {
        if (instance == null) {
            instance = new SubmissionResultDisplayer();
        }
        return instance;
    }
    
    private ConvenientDialogDisplayer dialogs;
    
    /*package*/ SubmissionResultDisplayer() {
        this.dialogs = ConvenientDialogDisplayer.getDefault();
    }
    
    public void showResult(SubmissionResult result) {
        switch (result.getStatus()) {
            case OK:
                dialogs.displayHappyMessage("All tests passed!", "Yay!");
                break;
            case FAIL:
                displayTestFailures(result.getCategorizedTestFailures());
                break;
            case ERROR:
                displayError(result.getError());
                break;
        }
    }
    
    private void displayTestFailures(Map<String, List<String>> failures) {
        LongTextDisplayPanel panel = new LongTextDisplayPanel(testFailuresToHtml(failures));
        dialogs.showDialog(panel, NotifyDescriptor.ERROR_MESSAGE, "", false);
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
    
    private String testFailuresToHtml(Map<String, List<String>> failures) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<p>Some tests failed.</p>");
        sb.append("<ul>");
        
        String[] categories = failures.keySet().toArray(new String[0]);
        Arrays.sort(categories);
        for (String category : categories) {
            sb.append("<li>")
                    .append("<font color=\"red\">")
                    .append(StringEscapeUtils.escapeHtml4(category))
                    .append("</font>")
                    .append("<ul>");
            
            for (String msg : failures.get(category)) {
                sb.append("<li><font color=\"red\">")
                        .append(StringEscapeUtils.escapeHtml4(msg))
                        .append("</font></li>");
            }
            
            sb.append("</ul>")
                    .append("</li>");
        }
        sb.append("</ul>").append("</html>");
        return sb.toString();
    }
}
