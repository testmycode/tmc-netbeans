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
import org.netbeans.jellytools.actions.ActionNoBlock;
import org.netbeans.jellytools.actions.OpenAction;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JDialogOperator;

public class UpdatingExercisesTest extends TmcFunctionalTestCase {
    public static Test suite() {
        return loadSuite(UpdatingExercisesTest.class);
    }

    public UpdatingExercisesTest() {
        super("UpdatingExercisesTest");
    }

    public void testUpdatingExistingExercises() throws Exception {
        CourseFixture course = serverFixture.addDefaultCourse("TestCourse", "TestProject", getFixtureProjectDir("TestProject"));
        ExerciseFixture exercise = course.getExerciseFixture("TestProject");

        SettingsOperator.setAllSettings(this, "TestCourse");

        JDialogOperator downloadDialog = new JDialogOperator("Download exercises");
        new JButtonOperator(downloadDialog, "Download").doClick();
        Thread.sleep(2000);

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
//        serverFixture.updateServerCourseList();

        // No idea why, but if this is a plain Action then it usually (but not always)
        // clicks the menu item but then hangs and finally fails claiming to not be able to click it.
        new ActionNoBlock("TMC|Download/update exercises", null).perform();
        Thread.sleep(2000);

        JDialogOperator updateDialog = new JDialogOperator("Update exercises");
        Thread.sleep(1000);
        new JButtonOperator(updateDialog, "Update").doClick();
        Thread.sleep(2000);

        Node projectNode = ProjectsTabOperator.invoke().getProjectRootNode("TestProject");
        Node sourceFileNode = new Node(projectNode, "Test Packages|<default package>|MainTest.java");
        new OpenAction().performAPI(sourceFileNode);
        EditorOperator editor = new EditorOperator("MainTest.java");
        assertTrue("Editor shows updated test", editor.getText().contains("TROLOLOO"));
    }
}
