package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.model.SourceFileLookup;
import fi.helsinki.cs.tmc.testrunner.CaughtException;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.apache.commons.lang3.StringEscapeUtils;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.text.Line.ShowOpenType;
import org.openide.text.Line.ShowVisibilityType;

class TestResultPanel extends JPanel {
    private static final int PADDING_BETWEEN_BOXES = 4;
    
    private static final Logger log = Logger.getLogger(TestResultPanel.class.getName());
    
    private final SourceFileLookup sourceFileLookup;
    
    private boolean passedTestsVisible = false;
    private boolean allFailuresVisible = false;
    private List<TestCaseResult> storedResults = new ArrayList<TestCaseResult>();
    
    public TestResultPanel() {
        this.sourceFileLookup = SourceFileLookup.getDefault();
        this.setLayout(new GridBagLayout());
    }
    
    public void setTestCaseResults(List<TestCaseResult> results) {
        this.clear();
        storedResults.addAll(results);
        rebuildCells();
    }
    
    private void rebuildCells() {
        this.removeAll();
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.insets.bottom = PADDING_BETWEEN_BOXES;
        
        for (TestCaseResult result : storedResults) {
            if (!result.isSuccessful() || passedTestsVisible) {
                this.add(new TestCaseResultCell(result, sourceFileLookup), gbc);
                
                if (!allFailuresVisible && !result.isSuccessful()) {
                    break;
                }
            }
        }
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
    
    private void scrollToTop() {
        scrollRectToVisible(new Rectangle(0, 0, 1, 1));
    }
    
    public void clear() {
        storedResults.clear();
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
    
    private static class TestCaseResultCell extends JPanel {
        private static final Color FAIL_COLOR = new Color(0xED0000);
        private static final Color PASS_COLOR = new Color(0x6FD06D);
        private static final Color FAIL_TEXT_COLOR = FAIL_COLOR.darker();
        private static final Color PASS_TEXT_COLOR = PASS_COLOR.darker();
        
        private final TestCaseResult result;
        private final SourceFileLookup sourceFileLookup;
        private final JButton backtraceButton;
        private final GridBagConstraints gbc = new GridBagConstraints();

        public TestCaseResultCell(TestCaseResult result, SourceFileLookup sourceFileLookup) {
            this.result = result;
            this.sourceFileLookup = sourceFileLookup;
            
            this.setLayout(new GridBagLayout());
            gbc.gridx = 0;
            gbc.gridy = GridBagConstraints.RELATIVE;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            
            String passOrFail = result.isSuccessful() ? "PASS: " : "FAIL: ";
            SelectableText titleLabel = new SelectableText(passOrFail + result.getName());
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD).deriveFont(titleLabel.getFont().getSize2D() + 2));
            titleLabel.setForeground(getResultTextColor());
            
            this.add(titleLabel, gbc);
            
            if (result.getMessage() != null) {
                this.add(new SelectableText(result.getMessage()), gbc);
            }
            
            if (result.getException() != null) {
                add(Box.createVerticalStrut(16), gbc);
                this.backtraceButton = new JButton(backtraceAction);
                gbc.weighty = 1.0; // Leave it so for the backtrace
                this.add(backtraceButton, gbc);
            } else if (result.getBacktrace() != null) {
                add(Box.createVerticalStrut(16), gbc);
                this.backtraceButton = new JButton(valgrindAction);
                gbc.weighty = 1.0; // Leave it so for the backtrace
                this.add(backtraceButton, gbc);
            } else {
                this.backtraceButton = null;
            }
            
            this.setBorder(this.createBorder());
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
                this.setBackground(UIManager.getColor("Label.background"));
                
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
        
        private static class BacktraceDisplay extends JEditorPane {
            private String content;
            
            public BacktraceDisplay() {
                this.content = "";
                this.setEditable(false);
                this.setContentType("text/html");
                this.setBackground(UIManager.getColor("Label.background"));
            }

            public void setContent(String content) {
                this.content = "<html>" + 
                        StringEscapeUtils.escapeHtml3(content)
                        .replaceAll(" ", "&nbsp;")
                        .replaceAll("\n", "<br />")
                        .replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;") + 
                        "</html>";
            }
            
            public void finish() {
                this.setText(content);
            }
        }
        
        private Action valgrindAction = new AbstractAction("Show valgrind trace") {

            @Override
            public void actionPerformed(ActionEvent e) {
                remove(backtraceButton);
                
                BacktraceDisplay display = new BacktraceDisplay();
                String output = result.getBacktrace();
                display.setContent(output);
                display.finish();
                add(display, gbc);
                
                revalidate();
            }
            
        };
        
        private Action backtraceAction = new AbstractAction("Show backtrace") {
            @Override
            public void actionPerformed(ActionEvent e) {
                remove(backtraceButton);
                
                ExceptionDisplay display = new ExceptionDisplay();
                addException(display, result.getException(), false);
                display.finish();
                add(display, gbc);
                
                revalidate();
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
                    return;
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
        
        private Border createBorder() {
            Border innerPadding = BorderFactory.createEmptyBorder(5, 10, 5, 5);
            Border leftColorBar = BorderFactory.createMatteBorder(0, 6, 0, 0, getResultColor());
            
            return BorderFactory.createCompoundBorder(leftColorBar, innerPadding);
        }
    }
    
}
