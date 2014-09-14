package fi.helsinki.cs.tmc.functionaltests;

import fi.helsinki.cs.tmc.functionaltests.utils.TmcFunctionalTestCase;
import fi.helsinki.cs.tmc.functionaltests.utils.SettingsOperator;
import junit.framework.Test;

public class SavingSettingsTest extends TmcFunctionalTestCase {

    public static Test suite() {
        return loadSuite(SavingSettingsTest.class);
    }
    
    public SavingSettingsTest() {
        super("SavingSettingsTest");
    }
    
    public void testSettingsGetSaved() throws Exception {
        serverFixture.addEmptyCourse("Course1");
        serverFixture.addEmptyCourse("Course2");
        
        SettingsOperator settings = SettingsOperator.openSettingsDialog();
        assertFalse(settings.getSavePasswordCheckbox().isSelected());
        settings.getUsernameField().setText(serverFixture.expectedUser);
        settings.getPasswordField().setText(serverFixture.expectedPassword);
        settings.getSavePasswordCheckbox().doClick();
        settings.getServerAddressField().setText(serverFixture.getFakeServer().getBaseUrl());
        
        serverFixture.getFakeServer().waitForRequestToComplete(); // Wait for course list to load
        Thread.sleep(500);
        
        settings.getCourseList().setSelectedIndex(1);
        
        settings.clickOk();
        
        settings = SettingsOperator.openSettingsDialog();
        settings.getUsernameField().setText("anotheruser");
        settings.getSavePasswordCheckbox().doClick();
        settings.clickCancel();
        
        settings = SettingsOperator.openSettingsDialog();
        assertEquals(serverFixture.expectedUser, settings.getUsernameField().getText());
        assertEquals(serverFixture.expectedPassword, new String(settings.getPasswordField().getPassword()));
        assertTrue(settings.getSavePasswordCheckbox().isSelected());
        assertEquals(1, settings.getCourseList().getSelectedIndex());
        settings.clickCancel();
    }
}
