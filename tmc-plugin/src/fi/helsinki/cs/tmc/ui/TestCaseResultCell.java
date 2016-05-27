package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.langs.domain.TestResult;
import fi.helsinki.cs.tmc.model.SourceFileLookup;
import fi.helsinki.cs.tmc.testrunner.CaughtException;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;

import com.google.common.collect.ImmutableList;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.commons.lang3.StringEscapeUtils;

import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.Line;

public final class TestCaseResultCell {

    private static final Logger log = Logger.getLogger(TestCaseResultCell.class.getName());

    private static final Color FAIL_COLOR = new Color(0xED0000);
    private static final Color PASS_COLOR = new Color(0x6FD06D);
    private static final Color VALGRIND_FAILED_COLOR = new Color(0xFFD000);
    private static final Color FAIL_TEXT_COLOR = FAIL_COLOR.darker();
    private static final Color PASS_TEXT_COLOR = PASS_COLOR.darker();

    private final Exercise exercise;
    private final TestResult result;
    private final SourceFileLookup sourceFileLookup;
    private JButton detailedMessageButton;
    private final GridBagConstraints gbc = new GridBagConstraints();
    private final JPanel detailView;
    private final ResultCell resultCell;

    public TestCaseResultCell(final Exercise exercise, final TestResult result, final SourceFileLookup sourceFileLookup) {

        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        this.exercise = exercise;
        this.result = result;
        this.sourceFileLookup = sourceFileLookup;
        this.detailView = createDetailView();

        final String title = (result.passed ? "PASS: " : "FAIL: ") + result.name;

        this.resultCell = new ResultCell(getResultColor(),
                                         getResultTextColor(),
                                         title,
                                         result.errorMessage,
                                         detailView);
    }

    public ResultCell getCell() {

        return resultCell;
    }

    private JPanel createDetailView() {

        final JPanel view = new JPanel();

        view.setLayout(new GridBagLayout());
        view.setBackground(Color.WHITE);

        if (result.backtrace != null) {
            view.add(Box.createVerticalStrut(16), gbc);
            this.detailedMessageButton = new JButton(detailedMessageAction);
            gbc.weighty = 1.0; // Leave it so for the detailed message
            view.add(detailedMessageButton, gbc);
        } else if (result.errorMessage != null) {
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
                sb.append(lines[i]).append("<br/>");
            }
            sb.append(lines[lines.length - 1]);
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
            display.setBackground(Color.WHITE);
            display.setContent(result.errorMessage);
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
// TODO 
//            addException(display, result.backtrace, false);
            display.finish();

            detailView.add(display, gbc);
            

            resultCell.revalidate();
            resultCell.repaint();
        }

//        private void addException(ExceptionDisplay display, ImmutableList<String> ex, boolean isCause) {
//            String mainLine;
//            if (ex.message != null) {
//                mainLine = ex.className + ": " + ex.message;
//            } else {
//                mainLine = ex.className;
//            }
//            if (isCause) {
//                mainLine = "Caused by: " + mainLine;
//            }
//            display.addBoldTextLine(mainLine);
//
//            addStackTraceLines(display, ex.stackTrace);
//
//            if (ex.cause != null) {
//                addException(display, ex.cause, true);
//            }
//        }

        private void addStackTraceLines(ExceptionDisplay display, StackTraceElement[] stackTrace) {
            for (final StackTraceElement ste : stackTrace) {
                final FileObject sourceFile = sourceFileLookup.findSourceFileFor(exercise, ste.getClassName());

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
                            line.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
                        }
                    }
                }
            } catch (Exception ex) {
                ExceptionUtils.logException(ex, log, Level.WARNING);
            }
        }
    };

    private Color getResultColor() {
//        if (valgrindFailed) {
//            return VALGRIND_FAILED_COLOR;
//        } else
        if (result.passed) {
            return PASS_COLOR;
        } else {
            return FAIL_COLOR;
        }
    }

    private Color getResultTextColor() {
        if (result.passed) {
            return PASS_TEXT_COLOR;
        } else {
            return FAIL_TEXT_COLOR;
        }
    }
}
