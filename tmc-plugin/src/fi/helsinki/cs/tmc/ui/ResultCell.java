package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.util.Exceptions;

public final class ResultCell extends JPanel {

    private final GridBagConstraints constraints = new GridBagConstraints();
    private Color titleColor, borderColor;

    public ResultCell(final Color borderColor,
            final Color titleColor,
            final FileObject fileObject,
            final String message,
            final JPanel detailView) {

        createStyle(borderColor, titleColor);
        createTitle(fileObject);
        createBody(message, detailView);
    }

    public ResultCell(final Color borderColor,
            final Color titleColor,
            final String title,
            final String message,
            final JPanel detailView) {

        createStyle(borderColor, titleColor);
        createTitle(title);
        createBody(message, detailView);

    }

    private void createBody(final String message, final JPanel detailView) {

        createMessage(message);
        createDetailView(detailView);
    }

    private void createStyle(final Color borderColor, final Color titleColor) {

        this.borderColor = borderColor;
        this.titleColor = titleColor;
        this.setLayout(new GridBagLayout());
        this.setBackground(Color.WHITE);
        createConstraints();
        createBorder();
    }

    private void createConstraints() {

        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.gridx = 0;
        constraints.gridy = GridBagConstraints.RELATIVE;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
    }

    private void createTitle(final String title) {

        SelectableText titleLabel = new SelectableText(title);

        titleLabel.setFont(titleLabel.getFont()
                .deriveFont(Font.BOLD)
                .deriveFont(titleLabel.getFont().getSize2D() + 2));

        titleLabel.setForeground(titleColor);

        this.add(titleLabel, constraints);
    }

    private void createTitle(final FileObject fileObject) {

        SelectableText titleLabel = new SelectableText(fileObject.getName());

        titleLabel.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (fileObject.isValid()) {
                    DataObject dataObject;
                    try {
                        dataObject = DataObject.find(fileObject);
                    } catch (DataObjectNotFoundException ex) {
                        // Should propably log something somewhere
                        return;
                    }

                    EditorCookie editorCookie = dataObject.getLookup().lookup(EditorCookie.class);
                    if (editorCookie != null) {
                        editorCookie.open(); // Asynchronous
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        titleLabel.setFont(titleLabel.getFont()
                .deriveFont(Font.BOLD)
                .deriveFont(titleLabel.getFont().getSize2D() + 2));

        titleLabel.setForeground(titleColor);

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
        Border leftColorBar = BorderFactory.createMatteBorder(0, 6, 0, 0, borderColor);

        this.setBorder(BorderFactory.createCompoundBorder(leftColorBar, innerPadding));
    }
}
