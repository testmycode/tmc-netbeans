package fi.helsinki.cs.tmc.ui;

import java.awt.Component;
import hy.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.data.FeedbackAnswer;
import hy.tmc.core.domain.submission.FeedbackQuestion;
import hy.tmc.core.domain.submission.SubmissionResult;
import fi.helsinki.cs.tmc.ui.feedback.FeedbackQuestionPanel;
import fi.helsinki.cs.tmc.ui.feedback.FeedbackQuestionPanelFactory;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.openide.awt.HtmlBrowser;
import static fi.helsinki.cs.tmc.ui.Boxer.*;

public class SuccessfulSubmissionDialog extends JDialog {
    
    private static final Logger log = Logger.getLogger(SuccessfulSubmissionDialog.class.getName());
    
    private JButton okButton;
    
    private List<FeedbackQuestionPanel> feedbackQuestionPanels;

    public SuccessfulSubmissionDialog(Exercise exercise, SubmissionResult result) {
        this.setTitle(exercise.getName() + " passed");
        
        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        setContentPane(contentPane);
        
        addYayLabel();
        addVSpace(6);
        if (exercise.requiresReview() && !result.getMissingReviewPoints().isEmpty()) {
            addRequiresReviewLabels();
        }
        addVSpace(6);
        addPointsLabel(result);
        addVSpace(10);
        addModelSolutionButton(result);
        addVSpace(20);
        addFeedbackQuestions(result); //TODO: maybe put in box
        addVSpace(10);
        addOkButton();
        
        pack();
    }
    
    public void addOkListener(ActionListener okListener) {
        this.okButton.addActionListener(okListener);
    }
    
    public List<FeedbackAnswer> getFeedbackAnswers() {
        List<FeedbackAnswer> answers = new ArrayList<FeedbackAnswer>();
        for (FeedbackQuestionPanel panel : feedbackQuestionPanels) {
            FeedbackAnswer answer = panel.getAnswer();
            if (answer != null) {
                answers.add(answer);
            }
        }
        return answers;
    }
    
    private void addVSpace(int height) {
        add(Box.createVerticalStrut(height));
    }
    
    private Box leftAligned(Component component) {
        return hbox(component, hglue());
    }
    
    private void addYayLabel() {
        JLabel yayLabel = new JLabel("All tests passed on the server.");
        
        Font font = yayLabel.getFont();
        font = font.deriveFont(Font.BOLD, font.getSize2D() * 1.2f);
        yayLabel.setFont(font);
        
        yayLabel.setForeground(new java.awt.Color(0, 153, 51));
        yayLabel.setIcon(ConvenientDialogDisplayer.getDefault().getSmileyIcon());
        
        getContentPane().add(leftAligned(yayLabel));
    }
    
    private void addRequiresReviewLabels() {
        JLabel lbl1 = new JLabel("This exercise requires a code review.");
        JLabel lbl2 = new JLabel("It will have a yellow marker until it's accepted by an instructor.");
        getContentPane().add(leftAligned(lbl1));
        getContentPane().add(leftAligned(lbl2));
    }
    
    private void addPointsLabel(SubmissionResult result) {
        JLabel pointsLabel = new JLabel(getPointsMsg(result));
        pointsLabel.setFont(pointsLabel.getFont().deriveFont(Font.BOLD));
        
        getContentPane().add(leftAligned(pointsLabel));
    }
    
    private String getPointsMsg(SubmissionResult result) {
        if (!result.getPoints().isEmpty()) {
            String msg = "Points permanently awarded: " + StringUtils.join(result.getPoints(), ", ") + ".";
            return "<html>" + StringEscapeUtils.escapeHtml4(msg).replace("\n", "<br />\n") + "</html>";
        } else {
            return "";
        }
    }

    private void addModelSolutionButton(SubmissionResult result) {
        if (result.getSolutionUrl() != null) {
            final String solutionUrl = result.getSolutionUrl();
            JButton solutionButton = new JButton(new AbstractAction("View model solution") {
                @Override
                public void actionPerformed(ActionEvent ev) {
                    try {
                        HtmlBrowser.URLDisplayer.getDefault().showURLExternal(new URL(solutionUrl));
                    } catch (Exception ex) {
                        ConvenientDialogDisplayer.getDefault().displayError("Failed to open browser.\n" + ex.getMessage());
                    }
                }
            });
            
            getContentPane().add(leftAligned(solutionButton));
        }
    }

    private void addFeedbackQuestions(SubmissionResult result) {
        this.feedbackQuestionPanels = new ArrayList<FeedbackQuestionPanel>();
        
        if (!result.getFeedbackQuestions().isEmpty() && result.getFeedbackAnswerUrl() != null) {
            for (FeedbackQuestion question : result.getFeedbackQuestions()) {
                try {
                    FeedbackQuestionPanel panel = FeedbackQuestionPanelFactory.getPanelForQuestion(question);
                    feedbackQuestionPanels.add(panel);
                } catch (IllegalArgumentException e) {
                    log.warning(e.getMessage());
                    continue;
                }
            }
            
            if (!feedbackQuestionPanels.isEmpty()) { // Some failsafety
                JLabel feedbackLabel = new JLabel("Feedback (leave empty to not send)");
                feedbackLabel.setFont(feedbackLabel.getFont().deriveFont(Font.BOLD));
                getContentPane().add(leftAligned(feedbackLabel));
                
                for (FeedbackQuestionPanel panel : feedbackQuestionPanels) {
                    getContentPane().add(leftAligned(panel));
                }
            } else {
                feedbackQuestionPanels = null;
            }
        }
    }
    
    private void addOkButton() {
        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });
        getContentPane().add(hbox(hglue(), okButton));
    }
    
}
