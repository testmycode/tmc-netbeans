package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.core.domain.Exercise;

import org.openide.filesystems.FileUtil;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class ProgressPanel extends JPanel {

    public ProgressPanel(Exercise exercise) {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        addTextLabel();
        addProgressBar(exercise);
        this.add(Boxer.vstrut(10));
        addColorDescriptions();
        addDetailsButton();
    }

    private void addTextLabel() {
        this.add(new JLabel("Your week's progress:"));
    }

    private void addProgressBar(Exercise exercise) {
        JProgressBar progressBar = new ProgressBar(exercise);
        progressBar.setAlignmentX(CENTER_ALIGNMENT);
        this.add(progressBar);
    }

    private void addDetailsButton() {
        JButton button = new JButton("More details");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Action timelineAction = FileUtil.getConfigObject("Actions/TMC/timeline-Timeline.instance", Action.class);
                timelineAction.actionPerformed(e);
            }
        });
        this.add(button);
    }

    private void addColorDescriptions() {
        JPanel progressDescription = new ProgressBarDescription(0, 0, "Progress", Color.green);
        JPanel exerciseDescription = new ProgressBarDescription(0, 5, "Exercises", Color.yellow);
        JPanel skillDescription = new ProgressBarDescription(0, 10, "Skills", Color.orange);
        this.add(progressDescription);
        this.add(exerciseDescription);
        this.add(skillDescription);
    }
}
