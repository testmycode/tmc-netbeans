package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.TestCaseResult;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

@TopComponent.Description(preferredID=TestResultWindow.PREFERRED_ID, persistenceType=TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode="output", openAtStartup=false)
class TestResultWindow extends TopComponent {
    public static final String PREFERRED_ID = "TestResultWindow";
    
    private final JCheckBox showAllCheckbox;
    private final TestColorBar testColorBar;
    private final TestResultPanel resultPanel;
    
    public TestResultWindow() {
        this.setName("TMC Test Results");
        this.setDisplayName("TMC Test Results");
        
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        this.showAllCheckbox = new JCheckBox("Show all tests", false);
        showAllCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                resultPanel.setAllFailuresVisible(showAllCheckbox.isSelected());
                resultPanel.setPassedTestsVisible(showAllCheckbox.isSelected());
            }
        });
        
        this.testColorBar = new TestColorBar();
        testColorBar.setMinimum(0);
        testColorBar.setIndeterminate(true);
        testColorBar.setPreferredSize(new Dimension(300, 30));
        
        Box topPanel = Box.createHorizontalBox();
        topPanel.add(testColorBar);
        topPanel.add(showAllCheckbox);
        topPanel.add(Box.createHorizontalGlue());
        topPanel.setMinimumSize(new Dimension(topPanel.getMinimumSize().width, 40));
        
        this.resultPanel = new TestResultPanel();
        JScrollPane scrollPane = new JScrollPane(
                resultPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        
        this.add(topPanel);
        this.add(useMaxHeight(scrollPane));
    }
    
    private Component useMaxHeight(Component c) {
        Box box = Box.createHorizontalBox();
        box.add(Box.createVerticalGlue());
        box.add(c);
        return box;
    }
    
    public static TestResultWindow get() {
        TopComponent window = WindowManager.getDefault().findTopComponent("TestResultWindow");
        if (window instanceof TestResultWindow) {
            return (TestResultWindow)window;
        } else {
            throw new IllegalStateException("No TestResultWindow in WindowManager registry.");
        }
    }
    
    public void clear() {
        resultPanel.clear();
        testColorBar.setIndeterminate(true);
    }

    public void setTestCaseResults(List<TestCaseResult> results) {
        resultPanel.setTestCaseResults(results);
        testColorBar.setMaximum(results.size());
        testColorBar.setValue(countSuccessfulTests(results));
        testColorBar.setIndeterminate(false);
    }

    private int countSuccessfulTests(List<TestCaseResult> results) {
        int count = 0;
        for (TestCaseResult result : results) {
            if (result.isSuccessful()) {
                count += 1;
            }
        }
        return count;
    }
}
