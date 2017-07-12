package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.core.domain.Exercise;

import java.awt.Color;
import javax.swing.BoxLayout;
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
    }

    private void addTextLabel() {
        this.add(new JLabel("Your week's progress:"));
    }

    private void addProgressBar(Exercise exercise) {
        JProgressBar progressBar = new ProgressBar(exercise);
        progressBar.setAlignmentX(CENTER_ALIGNMENT);
        this.add(progressBar);
    }

    private void addColorDescriptions() {
        JPanel progressDescription = new ProgressBarDescription(0, 0, "Progress", ProgressBar.FINISHED);
        JPanel exerciseDescription = new ProgressBarDescription(0, 5, "Exercises", ProgressBar.UNFINISHED);
        JPanel skillDescription = new ProgressBarDescription(0, 10, "Skills", ProgressBar.ADAPTIVE);
        this.add(progressDescription);
        this.add(exerciseDescription);
        this.add(skillDescription);
    }
}
