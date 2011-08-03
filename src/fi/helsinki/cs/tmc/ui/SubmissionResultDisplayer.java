package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.SubmissionResult;
import java.awt.Color;
import java.util.List;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
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
                displayTestFailures(result.getTestFailures());
                break;
            case ERROR:
                displayError(result.getError());
                break;
        }
    }
    
    private void displayTestFailures(List<String> failures) {
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
    
    private String testFailuresToHtml(List<String> failures) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<p>Some tests failed.</p>");
        sb.append("<ul>");
        for (String failure : failures) {
            sb.append("<li><font color=\"red\">")
                    .append(StringEscapeUtils.escapeHtml4(failure))
                    .append("</font></li>");
        }
        sb.append("</ul>").append("</html>");
        return sb.toString();
    }
}
