package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.TestCaseResult;
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
        private static final Color FAIL_COLOR = new Color(0xFF8383);
        private static final Color PASS_COLOR = new Color(0x6FD06D);
        
        private final TestCaseResult result;
        private final JButton backtraceButton;

        public TestCaseResultCell(TestCaseResult result) {
            this.result = result;
            
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            
            this.add(Box.createHorizontalGlue()); // Maximize component width
            
            String passFail = result.isSuccessful() ? "PASS: " : "FAIL: ";
            JLabel titleLabel = new JLabel(passFail + result.getName());
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            this.add(titleLabel);
            
            if (result.getMessage() != null) {
                this.add(new JLabel(result.getMessage()));
            }
            
            if (result.getStackTrace() != null) {
                this.backtraceButton = new JButton(backtraceAction);
                add(Box.createVerticalStrut(16));
                this.add(backtraceButton);
            } else {
                this.backtraceButton = null;
            }
            
            this.setBackground(getBgColor());
            
            this.setBorder(this.createBorder());
        }
        
        private Action backtraceAction = new AbstractAction("Show backtrace") {
            @Override
            public void actionPerformed(ActionEvent e) {
                remove(backtraceButton);
                add(new JLabel(stackTraceToString(result.getStackTrace())));
                revalidate();
            }
            
            private String stackTraceToString(StackTraceElement[] stackTrace) {
                StringBuilder sb = new StringBuilder();
                sb.append("<html>");
                for (StackTraceElement ste : stackTrace) {
                    sb.append(StringEscapeUtils.escapeHtml3(ste.toString())).append("<br/>");
                }
                sb.append("</html>");
                return sb.toString();
            }
        };
        
        private Color getBgColor() {
            if (result.isSuccessful()) {
                return PASS_COLOR;
            } else {
                return FAIL_COLOR;
            }
        }
        
        private Border createBorder() {
            Border innerPadding = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            Border deco = BorderFactory.createLineBorder(getBgColor().darker());
            
            return BorderFactory.createCompoundBorder(deco, innerPadding);
        }
    }
    
}
