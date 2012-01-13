package fi.helsinki.cs.tmc.functionaltests;

import fi.helsinki.cs.tmc.functionaltests.utils.FullServerFixture.CourseFixture;
import fi.helsinki.cs.tmc.functionaltests.utils.FullServerFixture.ExerciseFixture;
import fi.helsinki.cs.tmc.functionaltests.utils.SettingsOperator;
import fi.helsinki.cs.tmc.functionaltests.utils.TmcFunctionalTestCase;
import fi.helsinki.cs.tmc.functionaltests.utils.ZipFilter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import junit.framework.Test;
import org.netbeans.jellytools.EditorOperator;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jellytools.actions.Action;
import org.netbeans.jellytools.actions.OpenAction;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.junit.NbModuleSuite;

public class UpdatingExercisesTest extends TmcFunctionalTestCase {
    public static Test suite() {
        return NbModuleSuite.allModules(UpdatingExercisesTest.class);
    }

    public UpdatingExercisesTest() {
        super("UpdatingExercisesTest");
    }

    public void testUpdatingExistingExercises() throws Exception {
        CourseFixture course = serverFixture.addDefaultCourse("TestCourse", getTestProjectZip());
        ExerciseFixture exercise = course.getExerciseFixture("TestExercise");

        SettingsOperator.setAllSettings(this, "TestCourse");

        JDialogOperator downloadDialog = new JDialogOperator("Download exercises");
        new JButtonOperator(downloadDialog, "Download").doClick();

        exercise.checksum = "new checksum";
        exercise.zipData = new ZipFilter() {
            @Override
            protected byte[] filterFile(ZipEntry zent, byte[] data) throws IOException {
                if (zent.getName().endsWith("MainTest.java")) {
                    return "//TROLOLOO".getBytes("UTF-8");
                } else {
                    return data;
                }
            }
        }.filter(exercise.zipData);
        serverFixture.updateServerCourseList();

        new Action("TMC|Check for new exercises / updates", null).perform();
        Thread.sleep(1000);

        JDialogOperator updateDialog = new JDialogOperator("Update exercises");
        new JButtonOperator(updateDialog, "Update").doClick();

        Node projectNode = ProjectsTabOperator.invoke().getProjectRootNode("TestProject");
        Node sourceFileNode = new Node(projectNode, "Test Packages|<default package>|MainTest.java");
        new OpenAction().performAPI(sourceFileNode);
        EditorOperator editor = new EditorOperator("MainTest.java");
        assertTrue("Editor shows updated test", editor.getText().contains("TROLOLOO"));
    }
}
