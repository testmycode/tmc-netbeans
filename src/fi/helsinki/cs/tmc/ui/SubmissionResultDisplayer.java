package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.SubmissionResult;
import java.util.List;

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
                String msg = testFailuresToString(result.getTestFailures());
                dialogs.displayLongMessage(msg);
                break;
            case ERROR:
                dialogs.displayLongMessage(result.getError());
                break;
        }
    }
    
    private String testFailuresToString(List<String> failures) {
        StringBuilder sb = new StringBuilder();
        for (String failure : failures) {
            sb.append(failure);
            sb.append("\n");
        }
        return sb.toString();
    }
}
