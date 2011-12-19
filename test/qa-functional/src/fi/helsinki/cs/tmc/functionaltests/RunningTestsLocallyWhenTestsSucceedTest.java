package fi.helsinki.cs.tmc.functionaltests;

import fi.helsinki.cs.tmc.functionaltests.utils.TmcFunctionalTestCase;
import junit.framework.Test;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jellytools.actions.Action;
import org.netbeans.junit.NbModuleSuite;

public class RunningTestsLocallyWhenTestsSucceedTest extends TmcFunctionalTestCase {

    public RunningTestsLocallyWhenTestsSucceedTest() {
        super("RunningTestsLocallyWhenTestsSucceedTest");
    }
    
    public static Test suite() {
        return NbModuleSuite.allModules(RunningTestsLocallyWhenTestsSucceedTest.class);
    }
    
    public void testRunningTestsLocallyWhenTestsSucceed() throws Exception {
        arrangeForCourseToBeDownloaded("TestCourse");

        ProjectsTabOperator.invoke().getProjectRootNode("TestProject").select();
        new Action("TMC|Run tests locally", null).performMenu();
        // It should succeed and display the "Submit to server?" dialog
        new NbDialogOperator("Submit?").btNo().doClick();
    }
}
