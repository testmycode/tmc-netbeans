package fi.helsinki.cs.tmc.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;

public final class ResultCell extends JPanel {

    private final GridBagConstraints gbc = new GridBagConstraints();
    private final Color resultColor;

    public ResultCell(final Color color, final String title, final String message, final JPanel detailedView) {

        this.resultColor = color;
        this.setLayout(new GridBagLayout());

        createConstraints();
        createTitle(title);
        createMessage(message);
        createDetailedView(detailedView);
        createBorder();
    }

    private void createConstraints() {

        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
    }

    private void createTitle(final String title) {

        SelectableText titleLabel = new SelectableText(title);

        titleLabel.setFont(titleLabel.getFont()
                                     .deriveFont(Font.BOLD)
                                     .deriveFont(titleLabel.getFont().getSize2D() + 2));

        titleLabel.setForeground(resultColor);

        this.add(titleLabel, gbc);
    }

    private void createMessage(final String message) {

        if (message != null) {
            this.add(new SelectableText(message), gbc);
        }
    }

    private void createDetailedView(final JPanel detailedView) {

        if (detailedView != null) {
            this.add(detailedView, gbc);
        }
    }

    private void createBorder() {

        Border innerPadding = BorderFactory.createEmptyBorder(5, 10, 5, 5);
        Border leftColorBar = BorderFactory.createMatteBorder(0, 6, 0, 0, resultColor);

        this.setBorder(BorderFactory.createCompoundBorder(leftColorBar, innerPadding));
    }
}
