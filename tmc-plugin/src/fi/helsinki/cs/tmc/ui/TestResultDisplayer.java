package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.FeedbackAnswer;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.langs.abstraction.Strategy;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.openide.NotifyDescriptor;

public class TestResultDisplayer {
    private static final Logger log = Logger.getLogger(TestResultDisplayer.class.getName());

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

    public void showSubmissionResult(Exercise exercise, SubmissionResult result, final ResultCollector resultCollector) {
        switch (result.getStatus()) {
            case OK:
                displayTestCases(result.getTestCases(), false, resultCollector);
                displaySuccessfulSubmissionMsg(exercise, result);
                break;
            case FAIL:
                displayTestCases(maybeAddValdrindToResults(result), false, resultCollector);
                displayFailedTestsMsg(exercise, result);
                break;
            case ERROR:
                clearTestCaseView();
                displayError(result.getError());
                break;
        }
    }

    private void displaySuccessfulSubmissionMsg(Exercise exercise, final SubmissionResult result) {
        final SuccessfulSubmissionDialog dialog = new SuccessfulSubmissionDialog(exercise, result);

        dialog.addOkListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<FeedbackAnswer> answers = dialog.getFeedbackAnswers();
                if (!answers.isEmpty()) {
                    CancellableCallable<String> task = new ServerAccess().getFeedbackAnsweringJob(result.getFeedbackAnswerUrl(), answers);
                    BgTask.start("Sending feedback", task, new BgTaskListener<String>() {
                        @Override
                        public void bgTaskReady(String result) {
                        }

                        @Override
                        public void bgTaskCancelled() {
                        }

                        @Override
                        public void bgTaskFailed(Throwable ex) {
                            String msg = "Failed to send feedback :-(\n" + ex.getMessage();
                            String msgWithBacktrace = msg + "\n" + ExceptionUtils.backtraceToString(ex);
                            log.log(Level.INFO, msgWithBacktrace);
                            dialogs.displayError(msg);
                        }
                    });
                }
            }
        });

        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        dialog.setLocationRelativeTo(null);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }

    private void displayFailedTestsMsg(Exercise exercise, SubmissionResult result) {

        String msg;
        boolean milderFail;

        if (!result.getPoints().isEmpty()) {
            msg = "Exercise " + exercise.getName() + " failed partially.\n";
            msg += "Points permanently awarded: " + StringUtils.join(result.getPoints(), ", ") + ".\n\n";
            milderFail = true;
        } else {
            msg = "Exercise " + exercise.getName() + " failed.\n";
            milderFail = false;
        }

        if (result.validationsFailed() && result.getValidationResult().getStrategy() == Strategy.FAIL) {
            msg += "There are validation errors.\n";
        }

        switch (result.getTestResultStatus()) {
            case ALL:
                msg += "All tests failed on the server.\nSee below.";
                break;
            case SOME:
                msg += "Some tests failed on the server.\nSee below.";
                break;
            case NONE:
                if (result.validationsFailed()) {
                    msg += "See below.";
                }
                break;
        }

        if (milderFail) {
            dialogs.displayWarning(msg);
        } else {
            dialogs.displayError(msg);
        }
    }

    /**
     * Shows local results and calls the callback if a submission should be started.
     */
    public void showLocalRunResult(final List<TestCaseResult> results,
                                   final boolean returnable,
                                   final Runnable submissionCallback,
                                   final ResultCollector resultCollector) {

        resultCollector.setSubmissionCallback(submissionCallback);

        displayTestCases(results, returnable, resultCollector);
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

    private void displayTestCases(final List<TestCaseResult> testCases, final boolean returnable, final ResultCollector resultCollector) {

        resultCollector.setReturnable(returnable);
        resultCollector.setTestCaseResults(testCases);
    }

    private void clearTestCaseView() {
        TestResultWindow.get().clear();
    }

    private List<TestCaseResult> maybeAddValdrindToResults(SubmissionResult result) {
        String valdrindOutput = result.getValgrindOutput();
        List<TestCaseResult> resultList = result.getTestCases();

        if (StringUtils.isNotBlank(valdrindOutput)) {
            TestCaseResult valgrindResult = new TestCaseResult("Valgrind validations", false , "Click show valgrind trace to view valgrind trace", valdrindOutput, true);
            resultList.set(0, valgrindResult);
        }

        return resultList;
    }

}
