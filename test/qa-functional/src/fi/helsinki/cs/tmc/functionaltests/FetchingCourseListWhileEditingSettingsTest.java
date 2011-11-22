package fi.helsinki.cs.tmc.functionaltests;

import fi.helsinki.cs.tmc.functionaltests.utils.TmcFunctionalTestCase;
import fi.helsinki.cs.tmc.functionaltests.utils.SettingsOperator;
import javax.swing.JComboBox;
import junit.framework.Test;
import org.netbeans.junit.NbModuleSuite;
import static fi.helsinki.cs.tmc.testing.JsonBuilder.*;

public class FetchingCourseListWhileEditingSettingsTest extends TmcFunctionalTestCase {
    public static Test suite() {
        return NbModuleSuite.allModules(FetchingCourseListWhileEditingSettingsTest.class);
    }
    
    public FetchingCourseListWhileEditingSettingsTest() {
        super("FetchingCourseListWhileEditingSettingsTest");
    }
    
    public void testFetchingCourseListInSettingsWindow() throws Exception {
        fakeServer.expectUser("theuser", "thepassword");
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
        
        settings.getUsernameField().setText("theuser");
        settings.getPasswordField().setText("thepassword");
        settings.getServerAddressField().setText(fakeServer.getBaseUrl());
        
        // Should make a request automatically once all fields are filled in
        fakeServer.waitForRequestToComplete();
        Thread.sleep(1000);
        
        JComboBox courseList = settings.getCourseList();
        assertEquals(2, courseList.getItemCount());
        assertEquals("Course1", courseList.getItemAt(0).toString());
        assertEquals("Course2", courseList.getItemAt(1).toString());
        
        settings.clickOk();
    }
}
