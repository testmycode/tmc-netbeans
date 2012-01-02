package fi.helsinki.cs.tmc.functionaltests.utils;

import org.netbeans.jemmy.operators.JPasswordFieldOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import javax.swing.SwingUtilities;
import java.io.IOException;
import org.netbeans.junit.NbTestCase;
import org.netbeans.jemmy.operators.JButtonOperator;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.netbeans.jellytools.actions.Action;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JFileChooserOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import static org.junit.Assert.*;

/**
 * Helpers on working with the settings dialog.
 */
public class SettingsOperator {
    private JDialogOperator dialog;

    public SettingsOperator(JDialogOperator dialog) {
        this.dialog = dialog;
    }
    
    public static SettingsOperator openSettingsDialog() {
        new Action("TMC|Settings", null).perform();
        return new SettingsOperator(new JDialogOperator("TMC Settings"));
    }
    
    public JDialogOperator getDialogOperator() {
        return dialog;
    }
    
    public JTextFieldOperator getUsernameField() {
        return new JTextFieldOperator(waitByLabel(JTextField.class, "Username"));
    }
    
    public JPasswordFieldOperator getPasswordField() {
        return new JPasswordFieldOperator(waitByLabel(JPasswordField.class, "Password"));
    }
    
    public JTextFieldOperator getServerAddressField() {
        return new JTextFieldOperator(waitByLabel(JTextField.class, "Server address"));
    }
    
    public JComboBoxOperator getCourseList() {
        return new JComboBoxOperator(waitByLabel(JComboBox.class, "Current course"));
    }
    
    public JCheckBoxOperator getSavePasswordCheckbox() {
        return new JCheckBoxOperator(dialog, "Save password");
    }
    
    public void clickOk() {
        // The OK handler may block so we need to background it
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new JButtonOperator(dialog, "OK").doClick();
            }
        });
    }
    
    public void clickCancel() {
        new JButtonOperator(dialog, "Cancel").doClick();
    }
    
    public void setProjectDownloadDirToTestWorkDir(NbTestCase testCase) throws IOException {
        // doClick blocks waiting for the file chooser so we send it to the background
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new JButtonOperator(dialog, "Browse").doClick();
            }
        });
        
        JFileChooserOperator fileChooser = new JFileChooserOperator();
        fileChooser.setSelectedFile(testCase.getWorkDir());
        fileChooser.approve();
    }
    
    public static void setAllSettings(TmcFunctionalTestCase testCase, String courseName) throws Exception {
        FullServerFixture serverFixture = testCase.serverFixture;
        SettingsOperator settings = openSettingsDialog();
        
        settings.setProjectDownloadDirToTestWorkDir(testCase);
        settings.getUsernameField().setText(serverFixture.expectedUser);
        settings.getPasswordField().setText(serverFixture.expectedPassword);
        settings.getServerAddressField().setText(serverFixture.getFakeServer().getBaseUrl());
        
        // Should make a request automatically once all fields are filled in
        serverFixture.getFakeServer().waitForRequestToComplete();
        Thread.sleep(1000);
        
        boolean foundCourse = false;
        JComboBoxOperator courseList = settings.getCourseList();
        for (int i = 0; i < courseList.getItemCount(); ++i) {
            if (courseList.getItemAt(i).toString().equals(courseName)) {
                courseList.setSelectedIndex(i);
                foundCourse = true;
                break;
            }
        }
        
        assertTrue("Course '" + courseName + "' not found in settings window", foundCourse);
        
        settings.clickOk();
    }
    
    @SuppressWarnings("unchecked")
    protected <T> T waitByLabel(Class<T> cls, String text) {
        JLabelOperator label = new JLabelOperator(dialog, text);
        return (T)label.getLabelFor();
    }
}
