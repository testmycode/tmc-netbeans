package fi.helsinki.cs.tmc.functionaltests;

import fi.helsinki.cs.tmc.functionaltests.utils.SettingsOperator;
import fi.helsinki.cs.tmc.functionaltests.utils.TmcFunctionalTestCase;
import junit.framework.Test;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JDialogOperator;

public class ProjectDownloadTest extends TmcFunctionalTestCase {
    public static Test suite() {
        return loadSuite(ProjectDownloadTest.class);
    }

    public ProjectDownloadTest() {
        super("ProjectDownloadTest");
    }
    
    public void testProjectDownloadAndExtraction() throws Exception {
        serverFixture.addDefaultCourse("TestCourse", "TestProject", getFixtureProjectDir("TestProject"));
        SettingsOperator.setAllSettings(this, "TestCourse");

        JDialogOperator downloadDialog = new JDialogOperator("Download exercises");
        new JButtonOperator(downloadDialog, "Download").doClick();
        
        Node projectNode = ProjectsTabOperator.invoke().getProjectRootNode("TestProject");
        Node sourceFileNode = new Node(projectNode, "Source Packages|<default package>|Main.java");
        assertTrue(sourceFileNode.isPresent());
    }
    
}
