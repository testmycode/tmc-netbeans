package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.langs.abstraction.ValidationResult;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class TestResultPanel extends JPanel {

    private static final int PADDING_BETWEEN_BOXES = 5;

    private boolean allTestsVisible = false;
    private Exercise exercise = null;
    private final List<TestCaseResult> storedTestCaseResults = new ArrayList<TestCaseResult>();
    private ValidationResult storedValidationResult;

    public TestResultPanel() {

        this.setLayout(new GridBagLayout());
    }

    public void setResults(final Exercise exercise, final List<TestCaseResult> results, final ValidationResult validationResult) {

        this.clear();

        this.exercise = exercise;
        storedTestCaseResults.addAll(results);
        storedValidationResult = validationResult;

        rebuildCells();
    }

    private void rebuildCells() {

        this.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.insets.top = PADDING_BETWEEN_BOXES;

        buildValidationCells(gbc);
        buildTestResultCells(gbc);

        gbc.weighty = 1.0;
        this.add(Box.createVerticalGlue(), gbc); // Minimize component heights

        this.revalidate();
        this.repaint();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrollToTop();
            }
        });
    }

    private void buildTestResultCells(final GridBagConstraints constraints) {

        for (ResultCell resultCell : TestResultBuilder.buildCells(exercise, storedTestCaseResults, allTestsVisible)) {
            this.add(resultCell, constraints);
        }
    }

    private void buildValidationCells(final GridBagConstraints constraints) {

        if (storedValidationResult == null) {
            return;
        }

        for (ResultCell resultCell : ValidationResultBuilder.buildCells(storedValidationResult)) {
            this.add(resultCell, constraints);
        }
    }

    private void scrollToTop() {
        scrollRectToVisible(new Rectangle(0, 0, 1, 1));
    }

    public void clear() {

        exercise = null;
        storedTestCaseResults.clear();

        this.removeAll();
        this.revalidate();
    }

    public void setAllTestsVisible(final boolean allTestsVisible) {

        this.allTestsVisible = allTestsVisible;

        rebuildCells();
    }
}
