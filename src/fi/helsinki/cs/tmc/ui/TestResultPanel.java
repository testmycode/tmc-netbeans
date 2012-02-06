package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.model.SourceFileLookup;
import fi.helsinki.cs.tmc.testrunner.CaughtException;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.text.StyledDocument;
import org.apache.commons.lang3.StringEscapeUtils;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
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
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }
    
    public void setTestCaseResults(List<TestCaseResult> results) {
        this.clear();
        storedResults.addAll(results);
        rebuildCells();
    }
    
    private void rebuildCells() {
        this.removeAll();
        for (TestCaseResult result : storedResults) {
            if (!result.isSuccessful() || passedTestsVisible) {
                this.add(new TestCaseResultCell(result, sourceFileLookup));
                this.add(Box.createVerticalStrut(PADDING_BETWEEN_BOXES));
                if (!allFailuresVisible && !result.isSuccessful()) {
                    break;
                }
            }
        }
        this.revalidate();
        this.repaint();
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

        public TestCaseResultCell(TestCaseResult result, SourceFileLookup sourceFileLookup) {
            this.result = result;
            this.sourceFileLookup = sourceFileLookup;
            
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            
            this.add(Box.createHorizontalGlue()); // Maximize component width
            
            String passFail = result.isSuccessful() ? "PASS: " : "FAIL: ";
            JLabel titleLabel = new JLabel(passFail + result.getName());
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD).deriveFont(titleLabel.getFont().getSize2D() + 2));
            titleLabel.setForeground(getResultTextColor());
            this.add(titleLabel);
            
            if (result.getMessage() != null) {
                this.add(new JLabel(messageToHtml(result.getMessage())));
            }
            
            if (result.getException() != null) {
                this.backtraceButton = new JButton(backtraceAction);
                add(Box.createVerticalStrut(16));
                this.add(backtraceButton);
            } else {
                this.backtraceButton = null;
            }
            
            this.setBorder(this.createBorder());
        }
        
        private String messageToHtml(String message) {
            return "<html>" + nlToBr(message) + "</html>";
        }

        private String nlToBr(String message) {
            StringBuilder sb = new StringBuilder();
            for (String line : message.split("\n")) {
                sb.append(escapeHtml(line.toString())).append("<br/>");
            }
            return sb.toString();
        }
        
        private String escapeHtml(String s) {
            return StringEscapeUtils.escapeHtml3(s);
        }
        
        private Action backtraceAction = new AbstractAction("Show backtrace") {
            @Override
            public void actionPerformed(ActionEvent e) {
                remove(backtraceButton);
                addException(result.getException(), false);
                revalidate();
            }
            
            private void addException(CaughtException ex, boolean isCause) {
                String mainLine;
                if (ex.message != null) {
                    mainLine = escapeHtml(ex.className) + ": " + nlToBr(ex.message);
                } else {
                    mainLine = escapeHtml(ex.className);
                }
                if (isCause) {
                    mainLine = "Caused by: " + mainLine;
                }
                mainLine = "<html>" + mainLine + "</html>";
                
                JLabel mainLineLabel = new JLabel(mainLine);
                mainLineLabel.setFont(mainLineLabel.getFont().deriveFont(Font.BOLD));
                add(mainLineLabel);
                
                addStackTraceLabels(ex.stackTrace);
                
                if (ex.cause != null) {
                    addException(ex.cause, true);
                }
            }
            
            private void addStackTraceLabels(StackTraceElement[] stackTrace) {
                for (final StackTraceElement ste : stackTrace) {
                    final FileObject sourceFile = sourceFileLookup.findSourceFileFor(ste.getClassName());
                    
                    if (sourceFile != null && ste.getLineNumber() > 0) {
                        HyperlinkLabel label = new HyperlinkLabel(ste.toString());
                        label.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                openAtLine(sourceFile, ste.getLineNumber());
                            }
                        });
                        add(label);
                    } else {
                        add(new JLabel(ste.toString()));
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
