package fi.helsinki.cs.tmc.functionaltests;

import fi.helsinki.cs.tmc.functionaltests.utils.TmcFunctionalTestCase;
import fi.helsinki.cs.tmc.functionaltests.utils.SettingsOperator;
import junit.framework.Test;
import org.netbeans.junit.NbModuleSuite;
import static fi.helsinki.cs.tmc.testing.JsonBuilder.*;

public class SavingSettingsTest extends TmcFunctionalTestCase {

    public static Test suite() {
        return NbModuleSuite.allModules(SavingSettingsTest.class);
    }
    
    public SavingSettingsTest() {
        super("SavingSettingsTest");
    }
    
    public void testSettingsGotSaved() throws Exception {
        fakeServer.respondWithCourses(
                object(
                    prop("api_version", "1"),
                    prop("courses", array(
                        object(
                            prop("name", "Course1"),
                            prop("exercises", array())
                        ),
                        object(
                            prop("name", "Course2"),
                            prop("exercises", array())
                        )
                    ))
                ).toString());
        
        SettingsOperator settings = SettingsOperator.openSettingsDialog();
        assertFalse(settings.getSavePasswordCheckbox().isSelected());
        settings.getUsernameField().setText("theuser");
        settings.getPasswordField().setText("thepassword");
        settings.getServerAddressField().setText(fakeServer.getBaseUrl());
        
        fakeServer.waitForRequestToComplete(); // Wait for course list to load
        Thread.sleep(500);
        
        settings.getCourseList().setSelectedIndex(1);
        
        settings.clickOk();
        
        settings = SettingsOperator.openSettingsDialog();
        settings.getUsernameField().setText("anotheruser");
        settings.clickCancel();
        
        settings = SettingsOperator.openSettingsDialog();
        assertEquals("theuser", settings.getUsernameField().getText());
        assertEquals(0, settings.getPasswordField().getPassword().length);
        assertFalse(settings.getSavePasswordCheckbox().isSelected());
        assertEquals(1, settings.getCourseList().getSelectedIndex());
        settings.clickCancel();
        
        settings = SettingsOperator.openSettingsDialog();
        settings.getPasswordField().setText("thenewpassword");
        settings.getSavePasswordCheckbox().doClick();
        settings.clickOk();
        
        settings = SettingsOperator.openSettingsDialog();
        assertTrue(settings.getSavePasswordCheckbox().isSelected());
        assertEquals("thenewpassword", new String(settings.getPasswordField().getPassword()));
        settings.clickCancel();
    }
}
