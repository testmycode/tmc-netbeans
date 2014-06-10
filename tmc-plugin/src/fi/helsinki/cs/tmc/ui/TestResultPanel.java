package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.model.SourceFileLookup;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.stylerunner.validation.ValidationError;
import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;
import fi.helsinki.cs.tmc.testrunner.CaughtException;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.apache.commons.lang3.StringEscapeUtils;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.text.Line.ShowOpenType;
import org.openide.text.Line.ShowVisibilityType;

public class TestResultPanel extends JPanel {

    private static final int PADDING_BETWEEN_BOXES = 5;

    private static final Logger log = Logger.getLogger(TestResultPanel.class.getName());

    private final SourceFileLookup sourceFileLookup;

    private boolean passedTestsVisible = false;
    private boolean allFailuresVisible = false;
    private final List<TestCaseResult> storedTestCaseResults = new ArrayList<TestCaseResult>();
    private final Map<File, List<ValidationError>> storedValidationResults = new HashMap<File, List<ValidationError>>();

    public TestResultPanel() {
        this.sourceFileLookup = SourceFileLookup.getDefault();
        this.setLayout(new GridBagLayout());
    }

    public void setResults(final List<TestCaseResult> results, final ValidationResult validationResults) {

        this.clear();

        storedTestCaseResults.addAll(results);
        storedValidationResults.putAll(validationResults.getValidationErrors());

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

    private void buildTestResultCells(final GridBagConstraints gbc) {

        for (TestCaseResult result : storedTestCaseResults) {

            if (!result.isSuccessful() || passedTestsVisible) {

                this.add(new TestCaseResultCell(result, sourceFileLookup).getCell(), gbc);

                if (!allFailuresVisible && !result.isSuccessful()) {
                    break;
                }
            }
        }
    }

    private void buildValidationCells(final GridBagConstraints constraints) {

        ResourceBundle bundle = ResourceBundle.getBundle("fi.helsinki.cs.tmc.resources.messages", TmcSettings.getDefault().getErrorMsgLocale());
        String line = bundle.getString("message.line");

        for (Map.Entry<File, List<ValidationError>> entry : storedValidationResults.entrySet()) {

            final File file = entry.getKey();
            final List<ValidationError> errors = entry.getValue();

            StringBuilder builder = new StringBuilder();

            for (ValidationError error : errors) {

                builder.append(String.format("%1$-10s", String.format(line, error.getLine())))
                       .append(" ")
                       .append(error.getMessage())
                       .append("\n");
            }

            final FileObject fileObject = GlobalPathRegistry.getDefault().findResource(file.toString());

            if (fileObject == null) {

                this.add(new ResultCell(new Color(0xFFD000),
                         Color.DARK_GRAY,
                         file.getName(),
                         builder.toString(),
                         null),
                         constraints);
            } else {

                this.add(new ResultCell(new Color(0xFFD000),
                         Color.DARK_GRAY,
                         fileObject,
                         builder.toString(),
                         null),
                         constraints);
            }
        }
    }

    private void scrollToTop() {
        scrollRectToVisible(new Rectangle(0, 0, 1, 1));
    }

    public void clear() {

        storedTestCaseResults.clear();
        storedValidationResults.clear();

        this.removeAll();
        this.revalidate();
    }

    public void setPassedTestsVisible(boolean passedTestsVisible) {
        this.passedTestsVisible = passedTestsVisible;
        rebuildCells();
    }

    public void setAllFailuresVisible(boolean allResultsVisible) {
        this.allFailuresVisible = allResultsVisible;
        rebuildCells();
    }

    private static class TestCaseResultCell {

        private static final Color FAIL_COLOR = new Color(0xED0000);
        private static final Color PASS_COLOR = new Color(0x6FD06D);
        private static final Color FAIL_TEXT_COLOR = FAIL_COLOR.darker();
        private static final Color PASS_TEXT_COLOR = PASS_COLOR.darker();

        private final TestCaseResult result;
        private final SourceFileLookup sourceFileLookup;
        private JButton detailedMessageButton;
        private final GridBagConstraints gbc = new GridBagConstraints();
        private final JPanel detailView;
        private final ResultCell resultCell;

        public TestCaseResultCell(final TestCaseResult result, final SourceFileLookup sourceFileLookup) {

            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.gridx = 0;
            gbc.weightx = 1.0;

            this.result = result;
            this.sourceFileLookup = sourceFileLookup;
            this.detailView = createDetailView();

            final String title = (result.isSuccessful() ? "PASS: " : "FAIL: ") + result.getName();

            this.resultCell = new ResultCell(getResultColor(),
                                             getResultTextColor(),
                                             title,
                                             result.getMessage(),
                                             detailView);
        }

        public JPanel getCell() {

            return resultCell;
        }

        private JPanel createDetailView() {

            final JPanel view = new JPanel();

            view.setLayout(new GridBagLayout());
            view.setBackground(Color.WHITE);

            if (result.getException() != null) {
                view.add(Box.createVerticalStrut(16), gbc);
                this.detailedMessageButton = new JButton(detailedMessageAction);
                gbc.weighty = 1.0; // Leave it so for the detailed message
                view.add(detailedMessageButton, gbc);
            } else if (result.getDetailedMessage() != null) {
                view.add(Box.createVerticalStrut(16), gbc);
                this.detailedMessageButton = new JButton(valgrindAction);
                gbc.weighty = 1.0; // Leave it so for the detailed message
                view.add(detailedMessageButton, gbc);
            } else {
                this.detailedMessageButton = null;
                return null;
            }

            return view;
        }

        private static class ExceptionDisplay extends JEditorPane {

            private StringBuilder htmlBuilder;
            private HashMap<String, ActionListener> linkHandlers;
            private int nextLinkId;

            public ExceptionDisplay() {
                this.htmlBuilder = new StringBuilder().append("<html><body>");
                this.linkHandlers = new HashMap<String, ActionListener>();
                this.nextLinkId = 1;

                this.setEditable(false);
                this.setContentType("text/html");
                this.setBackground(Color.WHITE);
                this.setFont(new JLabel().getFont());

                this.addHyperlinkListener(new HyperlinkListener() {
                    @Override
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            ActionListener listener = linkHandlers.get(e.getDescription());
                            if (listener != null) {
                                ActionEvent ae = new ActionEvent(ExceptionDisplay.this, ActionEvent.ACTION_PERFORMED, "link clicked");
                                listener.actionPerformed(ae);
                            }
                        }
                    }
                });
            }

            private String htmlText(String text) {
                return nlToBr(escapeHtml(text.trim()));
            }

            private static String escapeHtml(String s) {
                return StringEscapeUtils.escapeHtml3(s);
            }

            private static String nlToBr(String message) {
                StringBuilder sb = new StringBuilder();
                String[] lines = message.split("\n");
                for (int i = 0; i < lines.length - 1; ++i) {
                    sb.append(lines[i].toString()).append("<br/>");
                }
                sb.append(lines[lines.length - 1].toString());
                return sb.toString();
            }

            public void addTextLine(String text) {
                htmlBuilder.append(htmlText(text)).append("<br />");
            }

            public void addBoldTextLine(String text) {
                htmlBuilder.append("<b>").append(htmlText(text)).append("</b>").append("<br />");
            }

            public void addLink(String text, ActionListener listener) {
                htmlBuilder.append("<a href=\"#link").append(nextLinkId).append("\">").append(htmlText(text)).append("</a>").append("<br />");
                linkHandlers.put("#link" + nextLinkId, listener);
                nextLinkId += 1;
            }

            public void finish() {
                htmlBuilder.append("</body></html>");
                this.setText(htmlBuilder.toString());
                htmlBuilder = new StringBuilder();
            }
        }

        private static class DetailedMessageDisplay extends JEditorPane {

            private String content;

            public DetailedMessageDisplay() {
                this.content = "";
                this.setEditable(false);
                this.setContentType("text/html");
                this.setBackground(UIManager.getColor("Label.background"));
            }

            public void setContent(String content) {
                this.content = "<html>"
                        + StringEscapeUtils.escapeHtml3(content)
                        .replaceAll(" ", "&nbsp;")
                        .replaceAll("\n", "<br />")
                        .replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                        + "</html>";
            }

            public void finish() {
                this.setText(content);
            }
        }

        private Action valgrindAction = new AbstractAction("Show valgrind trace") {

            @Override
            public void actionPerformed(final ActionEvent event) {

                detailView.remove(detailedMessageButton);

                final DetailedMessageDisplay display = new DetailedMessageDisplay();
                display.setContent(result.getDetailedMessage());
                display.finish();

                detailView.add(display, gbc);

                resultCell.revalidate();
                resultCell.repaint();
            }

        };

        private Action detailedMessageAction = new AbstractAction("Show detailed message") {

            @Override
            public void actionPerformed(final ActionEvent event) {

                detailView.remove(detailedMessageButton);

                final ExceptionDisplay display = new ExceptionDisplay();
                addException(display, result.getException(), false);
                display.finish();

                detailView.add(display, gbc);

                resultCell.revalidate();
                resultCell.repaint();
            }

            private void addException(ExceptionDisplay display, CaughtException ex, boolean isCause) {
                String mainLine;
                if (ex.message != null) {
                    mainLine = ex.className + ": " + ex.message;
                } else {
                    mainLine = ex.className;
                }
                if (isCause) {
                    mainLine = "Caused by: " + mainLine;
                }
                display.addBoldTextLine(mainLine);

                addStackTraceLines(display, ex.stackTrace);

                if (ex.cause != null) {
                    addException(display, ex.cause, true);
                }
            }

            private void addStackTraceLines(ExceptionDisplay display, StackTraceElement[] stackTrace) {
                for (final StackTraceElement ste : stackTrace) {
                    final FileObject sourceFile = sourceFileLookup.findSourceFileFor(ste.getClassName());

                    if (sourceFile != null && ste.getLineNumber() > 0) {
                        display.addLink(ste.toString(), new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                openAtLine(sourceFile, ste.getLineNumber());
                            }
                        });
                    } else {
                        display.addTextLine(ste.toString());
                    }
                }
            }

            private void openAtLine(FileObject sourceFile, final int lineNum) {
                try {
                    if (sourceFile.isValid()) {
                        DataObject dataObject = DataObject.find(sourceFile);

                        EditorCookie editorCookie = dataObject.getLookup().lookup(EditorCookie.class);
                        if (editorCookie != null) {
                            editorCookie.open(); // Asynchronous

                            LineCookie lineCookie = dataObject.getCookie(LineCookie.class);
                            if (lineCookie != null) {
                                Line line = lineCookie.getLineSet().getCurrent(lineNum - 1);
                                line.show(ShowOpenType.OPEN, ShowVisibilityType.FOCUS);
                            }
                        }
                    }
                } catch (Exception ex) {
                    ExceptionUtils.logException(ex, log, Level.WARNING);
                }
            }
        };

        private Color getResultColor() {
            if (result.isSuccessful()) {
                return PASS_COLOR;
            } else {
                return FAIL_COLOR;
            }
        }

        private Color getResultTextColor() {
            if (result.isSuccessful()) {
                return PASS_TEXT_COLOR;
            } else {
                return FAIL_TEXT_COLOR;
            }
        }
    }

}
