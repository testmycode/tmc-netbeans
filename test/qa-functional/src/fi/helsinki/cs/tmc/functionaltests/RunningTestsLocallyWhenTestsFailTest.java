package fi.helsinki.cs.tmc.functionaltests;

import fi.helsinki.cs.tmc.functionaltests.utils.TmcFunctionalTestCase;
import junit.framework.Test;
import org.netbeans.jellytools.EditorOperator;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jellytools.actions.Action;
import org.netbeans.jellytools.actions.OpenAction;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.junit.NbModuleSuite;

public class RunningTestsLocallyWhenTestsFailTest extends TmcFunctionalTestCase {

    public RunningTestsLocallyWhenTestsFailTest() {
        super("RunningTestsLocallyWhenTestsFailTest");
    }
    
    public static Test suite() {
        return NbModuleSuite.allModules(RunningTestsLocallyWhenTestsFailTest.class);
    }

    public void testRunningTestsLocallyWhenTestsFail() throws Exception {
        arrangeForCourseToBeDownloaded("TestCourse");

        Node projectNode = ProjectsTabOperator.invoke().getProjectRootNode("TestProject");
        Node sourceFileNode = new Node(projectNode, "Source Packages|<default package>|Main.java");
        new OpenAction().performAPI(sourceFileNode);
        EditorOperator editor = new EditorOperator("Main.java");
        editor.select("ADD IMPL");
        int lineNumber = editor.getLineNumber();
        editor.deleteLine(lineNumber);
        editor.setCaretPositionToLine(lineNumber);
        editor.insert("return 0;\n");
        editor.save();

        new Action("TMC|Run tests locally", null).performMenu();

        // We expect to get a test result dialog with the test failures
        JDialogOperator dialog = new NbDialogOperator("Test results");
        new JButtonOperator(dialog, "OK").doClick();
    }
}
