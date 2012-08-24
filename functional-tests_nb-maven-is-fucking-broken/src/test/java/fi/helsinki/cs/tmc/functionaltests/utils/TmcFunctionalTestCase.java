package fi.helsinki.cs.tmc.functionaltests.utils;

import java.io.File;
import org.netbeans.jellytools.JellyTestCase;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JDialogOperator;

public abstract class TmcFunctionalTestCase extends JellyTestCase {

    protected FullServerFixture serverFixture;
    
    public TmcFunctionalTestCase(String testName) {
        super(testName);
        closeAllModal = true;
    }

    @Override
    protected void setUp() throws Exception {
        this.clearWorkDir();
        serverFixture = new FullServerFixture();
        super.setUp();
        dismissInitialSettingsDialog();
    }
    
    @Override
    protected void tearDown() throws Exception {
        this.clearWorkDir();
        serverFixture.tearDown();
        super.tearDown();
    }
    
    private void dismissInitialSettingsDialog() {
        JDialogOperator settingsDialog = new JDialogOperator("TMC Settings");
        new JButtonOperator(settingsDialog, "Cancel").doClick();
    }
    
    protected void arrangeForCourseToBeDownloaded(String courseName) throws Exception {
        serverFixture.addDefaultCourse(courseName, getTestProjectZip());
        SettingsOperator.setAllSettings(this, courseName);

        JDialogOperator downloadDialog = new JDialogOperator("Download exercises");
        new JButtonOperator(downloadDialog, "Download").doClick();
    }
    
    protected File getTestProjectZip() {
        return new File(getDataDir().getPath() + File.separator + "TestProject.zip");
    }
    
}
