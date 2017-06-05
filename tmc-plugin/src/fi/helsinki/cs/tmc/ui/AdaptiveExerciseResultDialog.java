package fi.helsinki.cs.tmc.ui;

import static fi.helsinki.cs.tmc.ui.Boxer.hbox;
import static fi.helsinki.cs.tmc.ui.Boxer.hglue;

import fi.helsinki.cs.tmc.actions.DownloadAdaptiveExerciseAction;
import fi.helsinki.cs.tmc.core.domain.submission.SubmissionResult;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.openide.awt.HtmlBrowser;

import java.awt.Dialog;

public class AdaptiveExerciseResultDialog extends JDialog {

    private static final Logger log = Logger.getLogger(AdaptiveExerciseResultDialog.class.getName());

    private JButton okButton;
    private JButton hateButton;
    private JCheckBox downloadNextExerciseButton;
    private JLabel feelingsLabel;

    public AdaptiveExerciseResultDialog(String exerciseName, SubmissionResult result) {
        this.setTitle("Adaptive exercise");

        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        setContentPane(contentPane);

        addHeadlineLabel(exerciseName);
        addVSpace(20);
        addSkillLabel();
        addVSpace(10);
        addDownloadBox();
        addVSpace(20);
        addInfoButton(result);
        addHateButton();
        addFeelingsLabel();
        addVSpace(20);
        addOkButton();

        pack();
        setSize(400, 300);
        setModalityType(Dialog.ModalityType.MODELESS);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        setVisible(true);
    }

    private void addVSpace(int height) {
        add(Box.createVerticalStrut(height));
    }

    private Box leftAligned(Component component) {
        return hbox(component, hglue());
    }

    private void addDownloadBox() {
        downloadNextExerciseButton =
                    new JCheckBox("Check and download next adaptive exercise");
        downloadNextExerciseButton.setSelected(true);
        getContentPane().add(leftAligned(downloadNextExerciseButton));
    }

    private void addSkillLabel() {
        JLabel skillText = new JLabel();
        skillText.setText("New skill granted!");
        getContentPane().add(leftAligned(skillText));
    }

    private void addHeadlineLabel(String exerciseName) {
        JLabel headline = new JLabel(exerciseName);

        headline.setIcon(ConvenientDialogDisplayer.getDefault().getSmileyIcon());

        Font font = headline.getFont();
        font = font.deriveFont(Font.BOLD, font.getSize2D() * 1.2f);
        headline.setFont(font);
        headline.setIcon(ConvenientDialogDisplayer.getDefault().getSmileyIcon());

        getContentPane().add(leftAligned(headline));
    }

    private void addOkButton() {
        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (downloadNextExerciseButton.isSelected()) {
                    new DownloadAdaptiveExerciseAction().actionPerformed(null);
                }

                setVisible(false);
                dispose();
            }
        });
        getContentPane().add(leftAligned(okButton));
    }

    private void addHateButton() {
        hateButton = new JButton("I hate this");
        hateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                feelingsLabel.setVisible(true);
            }
        });
        getContentPane().add(leftAligned(hateButton));
    }

    private void addFeelingsLabel() {
        feelingsLabel = new JLabel("Report your feelings to your instructor by email, please.");
        feelingsLabel.setVisible(false);
        getContentPane().add(leftAligned(feelingsLabel));
    }

    private void addInfoButton(SubmissionResult result) {
        final String infoUrl = "http://mooc.fi/adaptinfo";
        JButton infoButton = new JButton(new AbstractAction("More info about adaptive exercises") {
            @Override
            public void actionPerformed(ActionEvent ev) {
                try {
                    HtmlBrowser.URLDisplayer.getDefault().showURLExternal(new URL(infoUrl));
                } catch (Exception ex) {
                    ConvenientDialogDisplayer.getDefault().displayError("Failed to open browser.\n" + ex.getMessage());
                }
            }
        });

        getContentPane().add(leftAligned(infoButton));
    }
}
