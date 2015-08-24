package fi.helsinki.cs.tmc.actions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import fi.helsinki.cs.tmc.data.FeedbackAnswer;

import fi.helsinki.cs.tmc.model.NbTmcSettings;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.communication.HttpResult;
import fi.helsinki.cs.tmc.core.domain.submission.SubmissionResult;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Exceptions;

public class SendFeedbackAction {

    private static final Logger log = Logger.getLogger(SendFeedbackAction.class.getName());

    private final SubmissionResult result;

    private List<FeedbackAnswer> answers;
    private TmcCore core;
    private ConvenientDialogDisplayer dialogs;
    private NbTmcSettings settings = NbTmcSettings.getDefault();

    public SendFeedbackAction(List<FeedbackAnswer> answers, SubmissionResult result) {
        this.answers = answers;
        this.core = TmcCoreSingleton.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.result = result;
    }

    public void run() {
        try {
            ListenableFuture<HttpResult> feedbackFuture;
            feedbackFuture = core.sendFeedback(
                    getFeedbackAnswers(), new URI(result.getFeedbackAnswerUrl())
            );
            Futures.addCallback(feedbackFuture, new FeedbackReplyCallback());
        } catch (TmcCoreException ex) {
            Exceptions.printStackTrace(ex);
        } catch (URISyntaxException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private Map<String, String> getFeedbackAnswers() {
        Map<String, String> answerMap = new HashMap<String, String>();

        for (FeedbackAnswer answer : answers) {
            answerMap.put("" + answer.getQuestion().getId(), answer.getAnswer());
        }

        return answerMap;
    }

    private class FeedbackReplyCallback implements FutureCallback<HttpResult> {

        @Override
        public void onSuccess(HttpResult v) {
            System.out.println(v.getData() + "  " + v.getStatusCode());
        }

        @Override
        public void onFailure(Throwable ex) {
            String msg = "Failed to send feedback :-(\n" + ex.getMessage();
            String msgWithBacktrace = msg + "\n" + ExceptionUtils.backtraceToString(ex);
            log.log(Level.INFO, msgWithBacktrace);
            dialogs.displayError(msg);
        }
    }
}
