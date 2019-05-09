package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.actions.CheckForNewExercisesOrUpdates;
import fi.helsinki.cs.tmc.core.communication.TmcServerCommunicationTaskFactory;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.submission.FeedbackAnswer;
import fi.helsinki.cs.tmc.langs.abstraction.Strategy;
import fi.helsinki.cs.tmc.langs.domain.TestResult;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import fi.helsinki.cs.tmc.core.domain.submission.SubmissionResult;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.langs.domain.RunResult;

import com.google.common.collect.ImmutableList;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

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
        // Palvelimelta siis

        switch (result.getStatus()) {
            case OK:
                displayTestCases(magic(result), false, resultCollector);
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
            case HIDDEN:
                dialogs.displayMessage("Submission received and saved.\nResults are hidden for this exercise.");
                break;
        }
    }

    private void displaySuccessfulSubmissionMsg(Exercise exercise, final SubmissionResult result) {
        final SuccessfulSubmissionDialog dialog = new SuccessfulSubmissionDialog(exercise, result);

        dialog.addOkListener((ActionEvent e) -> {
            List<FeedbackAnswer> answers = dialog.getFeedbackAnswers();
            if (!answers.isEmpty()) {
                Callable<String> task = new TmcServerCommunicationTaskFactory().getFeedbackAnsweringJob(URI.create(result.getFeedbackAnswerUrl()), answers);
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
                        SwingUtilities.invokeLater(() -> {
                            dialogs.displayError(msg);
                        });
                    }
                });
            }
        });

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                saveCourseDbAndCheckForNewExercisesOrUpdates();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                saveCourseDbAndCheckForNewExercisesOrUpdates();
            }
        });

        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        dialog.setLocationRelativeTo(null);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }

    private void saveCourseDbAndCheckForNewExercisesOrUpdates() {
        Callable<Void> c = () -> {
            CourseDb.getInstance().save();
            new CheckForNewExercisesOrUpdates(true, false).run();
            return null;
        };
        BgTask.start("Saving local exercise progress", c);
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
            case ALL_FAILED:
                msg += "All tests failed on the server.\nSee below.";
                break;
            case SOME_FAILED:
                msg += "Some tests failed on the server.\nSee below.";
                break;
            case NONE_FAILED:
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
     * Shows local results and calls the callback if a submission should be
     * started.
     */
    public void showLocalRunResult(final RunResult runResult,
            final boolean returnable,
            final Runnable submissionCallback,
            final ResultCollector resultCollector) {

        resultCollector.setSubmissionCallback(submissionCallback);
        resultCollector.setReturnable(returnable);
        resultCollector.setLocalTestResults(runResult);
    }

    private void displayError(String error) {
        String htmlError
                = "<html><font face=\"monospaced\" color=\"red\">"
                + preformattedToHtml(error)
                + "</font></html>";
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

    private void displayTestCases(final ImmutableList<TestResult> testResults, final boolean returnable, final ResultCollector resultCollector) {

        resultCollector.setReturnable(returnable);
        resultCollector.setTestCaseResults(testResults);
    }

    private void clearTestCaseView() {
        TestResultWindow.get().clear();
    }

    private ImmutableList<TestResult> maybeAddValdrindToResults(SubmissionResult result) {
        String valdrindOutput = result.getValgrind();

        List<TestResult> resultList = result.getTestCases();

        // TODO: valgrind
        if (StringUtils.isNotBlank(valdrindOutput)) {

            TestResult valgrindResult = new TestResult("Valgrind validations", false, ImmutableList.<String>of(), "Click show valgrind trace to view valgrind trace", ImmutableList.copyOf(valdrindOutput.split("\n")));
            resultList.set(0, valgrindResult);
        }

        return ImmutableList.copyOf(resultList);
    }

    private ImmutableList<TestResult> magic(SubmissionResult result) {
        ImmutableList.Builder builder = ImmutableList.builder();
        for (TestResult testCase : result.getTestCases()) {
            builder.add(new TestResult(testCase.getName(),
                    testCase.isSuccessful(),
                    ImmutableList.copyOf(testCase.points),
                    testCase.getMessage(),
                    testCase.getException()));
        }
        return builder.build();
    }
}
