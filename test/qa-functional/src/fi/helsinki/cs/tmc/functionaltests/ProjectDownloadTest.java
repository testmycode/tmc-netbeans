package fi.helsinki.cs.tmc.functionaltests;

import fi.helsinki.cs.tmc.functionaltests.utils.SettingsOperator;
import fi.helsinki.cs.tmc.functionaltests.utils.TmcFunctionalTestCase;
import junit.framework.Test;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.junit.NbModuleSuite;

public class ProjectDownloadTest extends TmcFunctionalTestCase {
    public static Test suite() {
        return NbModuleSuite.allModules(ProjectDownloadTest.class);
    }

    public ProjectDownloadTest() {
        super("ProjectDownloadTest");
    }
    
    public void testProjectDownloadAndExtraction() throws Exception {
        serverFixture.addDefaultCourse("TestCourse", getTestProjectZip());
        SettingsOperator.setAllSettings(this, "TestCourse");
        
        new NbDialogOperator("Open exercises?").btYes().doClick();
        
        Node projectNode = ProjectsTabOperator.invoke().getProjectRootNode("TestProject");
        Node sourceFileNode = new Node(projectNode, "Source Packages|<default package>|Main.java");
        assertTrue(sourceFileNode.isPresent());
    }
    
}
