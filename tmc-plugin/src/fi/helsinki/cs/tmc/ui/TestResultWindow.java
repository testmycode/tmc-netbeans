package fi.helsinki.cs.tmc.ui;

import com.google.common.base.Function;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.openide.util.NbPreferences;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

@TopComponent.Description(preferredID=TestResultWindow.PREFERRED_ID, persistenceType=TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode="output", openAtStartup=false)
public class TestResultWindow extends TopComponent {

    public static final String PREFERRED_ID = "TestResultWindow";

    private static final Logger log = Logger.getLogger(TestResultWindow.class.getName());

    private final TestResultPanel resultPanel;
    private final JCheckBox showAllCheckbox;
    private final TestColorBar testColorBar;

    private ConvenientDialogDisplayer dialogDisplayer;

    public TestResultWindow() {

        this.dialogDisplayer = ConvenientDialogDisplayer.getDefault();

        this.setName("TMC Test Results");
        this.setDisplayName("TMC Test Results");

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.showAllCheckbox = new JCheckBox("Show all tests", false);
        showAllCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                resultPanel.setAllTestsVisible(showAllCheckbox.isSelected());
                saveWindowPreferences();
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

        resultPanel = new TestResultPanel();

        JPanel resultContainer = new JPanel();
        resultContainer.setLayout(new BoxLayout(resultContainer, BoxLayout.Y_AXIS));

        resultContainer.add(resultPanel);

        JScrollPane scrollPane = new JScrollPane(
                resultContainer,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);

        this.add(topPanel);
        this.add(usingMaxHeight(scrollPane));

        loadWindowPreferences();
    }

    private Component usingMaxHeight(Component c) {
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

    public void showResults(final List<TestCaseResult> testCaseResults,
                            final ValidationResult validationResults,
                            final Runnable submissionCallback,
                            final boolean submittable) {

        testColorBar.setMaximum(testCaseResults.size());
        testColorBar.setValue(countSuccessfulTests(testCaseResults));
        testColorBar.setIndeterminate(false);
        testColorBar.validationPass(validationResults.getValidationErrors().isEmpty());

        resultPanel.setResults(testCaseResults, validationResults);

        if (submittable) {

            dialogDisplayer.askYesNo("All tests passed. Submit to server?", "Submit?", new Function<Boolean, Void>() {

                @Override
                public Void apply(final Boolean yes) {

                    if (yes) {
                        submissionCallback.run();
                    }

                    return null;
                }
            });

        } else {
            this.openAtTabPosition(0);
            this.requestActive();
        }
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

    private void saveWindowPreferences() {
        Preferences prefs = NbPreferences.forModule(TestResultWindow.class);
        prefs.putBoolean("showAllTests", showAllCheckbox.isSelected());
        try {
            prefs.flush();
        } catch (BackingStoreException ex) {
            log.log(Level.WARNING, "Failed to save TestResultWindow preferences", ex);
        }
    }

    private void loadWindowPreferences() {
        Preferences prefs = NbPreferences.forModule(TestResultWindow.class);
        showAllCheckbox.setSelected(prefs.getBoolean("showAllTests", false));
    }
}
