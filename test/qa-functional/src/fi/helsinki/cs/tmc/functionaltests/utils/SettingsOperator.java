package fi.helsinki.cs.tmc.functionaltests.utils;

import javax.swing.JCheckBox;
import org.netbeans.jemmy.operators.JButtonOperator;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JDialog;
import javax.swing.JLabel;
import org.netbeans.jellytools.actions.Action;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import static org.junit.Assert.*;

/**
 * Helpers on working with the settings dialog.
 */
public class SettingsOperator {
    private JDialog dialog;

    public SettingsOperator(JDialog dialog) {
        this.dialog = dialog;
    }
    
    public static SettingsOperator openSettingsDialog() {
        new Action("TMC|Settings", null).perform();
        return new SettingsOperator(JDialogOperator.waitJDialog("TMC Settings", true, true));
    }

    public JDialog getDialog() {
        return dialog;
    }
    
    public JTextField getUsernameField() {
        return findByLabel(JTextField.class, "Username");
    }
    
    public JPasswordField getPasswordField() {
        return findByLabel(JPasswordField.class, "Password");
    }
    
    public JTextField getServerAddressField() {
        return findByLabel(JTextField.class, "Server address");
    }
    
    public JComboBox getCourseList() {
        return findByLabel(JComboBox.class, "Current course");
    }
    
    public JCheckBox getSavePasswordCheckbox() {
        return JCheckBoxOperator.findJCheckBox(dialog, "Save password", true, true);
    }
    
    public void clickOk() {
        JButtonOperator.findJButton(dialog, "OK", true, true).doClick();
    }
    
    public void clickCancel() {
        JButtonOperator.findJButton(dialog, "Cancel", true, true).doClick();
    }
    
    public static void setAllSettings(FullServerFixture serverFixture, String courseName) throws Exception {
        SettingsOperator settings = openSettingsDialog();
        
        settings.getUsernameField().setText(serverFixture.expectedUser);
        settings.getPasswordField().setText(serverFixture.expectedPassword);
        settings.getServerAddressField().setText(serverFixture.getFakeServer().getBaseUrl());
        
        // Should make a request automatically once all fields are filled in
        serverFixture.getFakeServer().waitForRequestToComplete();
        Thread.sleep(1000);
        
        boolean foundCourse = false;
        JComboBox courseList = settings.getCourseList();
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
    protected <T> T findByLabel(Class<T> cls, String text) {
        JLabel label = JLabelOperator.findJLabel(dialog, text, true, true);
        assertNotNull("Label with \"" + text + "\" not found", label);
        return (T)label.getLabelFor();
    }
}
