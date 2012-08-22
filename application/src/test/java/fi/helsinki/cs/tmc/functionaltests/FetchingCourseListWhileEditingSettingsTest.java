package fi.helsinki.cs.tmc.functionaltests;

import fi.helsinki.cs.tmc.functionaltests.utils.TmcFunctionalTestCase;
import fi.helsinki.cs.tmc.functionaltests.utils.SettingsOperator;
import junit.framework.Test;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.junit.NbModuleSuite;

public class FetchingCourseListWhileEditingSettingsTest extends TmcFunctionalTestCase {
    public static Test suite() {
        return NbModuleSuite.allModules(FetchingCourseListWhileEditingSettingsTest.class);
    }
    
    public FetchingCourseListWhileEditingSettingsTest() {
        super("FetchingCourseListWhileEditingSettingsTest");
    }
    
    public void testFetchingCourseListInSettingsWindow() throws Exception {
        serverFixture.addEmptyCourse("Course1");
        serverFixture.addEmptyCourse("Course2");
        
        SettingsOperator settings = SettingsOperator.openSettingsDialog();
        
        settings.getUsernameField().setText(serverFixture.expectedUser);
        settings.getPasswordField().setText(serverFixture.expectedPassword);
        settings.getServerAddressField().setText(serverFixture.getFakeServer().getBaseUrl());
        
        // Should make a request automatically once all fields are filled in
        serverFixture.getFakeServer().waitForRequestToComplete();
        Thread.sleep(1000);
        
        JComboBoxOperator courseList = settings.getCourseList();
        assertEquals(2, courseList.getItemCount());
        assertEquals("Course1", courseList.getItemAt(0).toString());
        assertEquals("Course2", courseList.getItemAt(1).toString());
        
        settings.clickOk();
    }
}
