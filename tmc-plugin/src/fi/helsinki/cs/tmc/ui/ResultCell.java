package fi.helsinki.cs.tmc.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;

public final class ResultCell extends JPanel {

    private final GridBagConstraints constraints = new GridBagConstraints();
    private final Color color;

    public ResultCell(final Color color, final String title, final String message, final JPanel detailView) {

        this.color = color;
        this.setLayout(new GridBagLayout());

        createConstraints();
        createTitle(title);
        createMessage(message);
        createDetailView(detailView);
        createBorder();
    }

    private void createConstraints() {

        constraints.gridx = 0;
        constraints.gridy = GridBagConstraints.RELATIVE;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
    }

    private void createTitle(final String title) {

        SelectableText titleLabel = new SelectableText(title);

        titleLabel.setFont(titleLabel.getFont()
                                     .deriveFont(Font.BOLD)
                                     .deriveFont(titleLabel.getFont().getSize2D() + 2));

        titleLabel.setForeground(color);

        this.add(titleLabel, constraints);
    }

    private void createMessage(final String message) {

        if (message != null) {
            this.add(new SelectableText(message), constraints);
        }
    }

    private void createDetailView(final JPanel detailView) {

        if (detailView != null) {
            this.add(detailView, constraints);
        }
    }

    private void createBorder() {

        Border innerPadding = BorderFactory.createEmptyBorder(5, 10, 5, 5);
        Border leftColorBar = BorderFactory.createMatteBorder(0, 6, 0, 0, color);

        this.setBorder(BorderFactory.createCompoundBorder(leftColorBar, innerPadding));
    }
}
