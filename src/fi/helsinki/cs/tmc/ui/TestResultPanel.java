package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.testrunner.CaughtException;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import org.apache.commons.lang3.StringEscapeUtils;

class TestResultPanel extends JPanel {
    private static final int PADDING_BETWEEN_BOXES = 4;
    
    private boolean passedTestsVisible = false;
    private boolean allFailuresVisible = false;
    private List<TestCaseResult> storedResults = new ArrayList<TestCaseResult>();
    
    public TestResultPanel() {
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
                this.add(new TestCaseResultCell(result));
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
        private final JButton backtraceButton;

        public TestCaseResultCell(TestCaseResult result) {
            this.result = result;
            
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
                JLabel stackTraceLabel = new JLabel(stackTraceToHtml(ex.stackTrace));
                
                add(mainLineLabel);
                add(stackTraceLabel);
                
                if (ex.cause != null) {
                    addException(ex.cause, true);
                }
            }
            
            private String stackTraceToHtml(StackTraceElement[] stackTrace) {
                StringBuilder sb = new StringBuilder();
                sb.append("<html>");
                for (StackTraceElement ste : stackTrace) {
                    sb.append(escapeHtml(ste.toString())).append("<br/>");
                }
                sb.append("</html>");
                return sb.toString();
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
