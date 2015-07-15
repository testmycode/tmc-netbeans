package fi.helsinki.cs.tmc.ui.feedback;

import hy.tmc.core.domain.submission.FeedbackQuestion;

public class FeedbackQuestionPanelFactory {
    public static FeedbackQuestionPanel getPanelForQuestion(FeedbackQuestion question) {
        if (question.isIntRange()) {
            return new IntRangeQuestionPanel(question);
        } else if (question.isText()) {
            return new TextQuestionPanel(question);
        } else {
            throw new IllegalArgumentException("Unknown feedback question type: " + question.getKind());
        }
    }
}
