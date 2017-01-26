package fi.helsinki.cs.tmc.functionaltests.utils;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestCase;
import org.netbeans.jellytools.JellyTestCase;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.junit.NbModuleSuite;

public abstract class TmcFunctionalTestCase extends JellyTestCase {

    protected FullServerFixture serverFixture;
    
    public TmcFunctionalTestCase(String testName) {
        super(testName);
        closeAllModal = true;
    }

    public static Test loadSuite(Class<? extends TestCase> cls) {
        return NbModuleSuite.create(cls, "ide|java|cnd|dlight|mavenmodules", ".*");
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
//        serverFixture.tearDown();
        super.tearDown();
    }
    
    private void dismissInitialSettingsDialog() {
        JDialogOperator settingsDialog = new JDialogOperator("TMC Settings");
        new JButtonOperator(settingsDialog, "Cancel").doClick();
    }
    
    protected void arrangeForCourseToBeDownloaded(String courseName, String projectName) throws Exception {
        serverFixture.addDefaultCourse(courseName, projectName, getFixtureProjectDir(projectName));
        SettingsOperator.setAllSettings(this, courseName);

        JDialogOperator downloadDialog = new JDialogOperator("Download exercises");
        new JButtonOperator(downloadDialog, "Download").doClick();
    }
    
    protected File getFixtureProjectDir(String projectName) {
        return new File(getDataDir().getPath() + File.separator + projectName + "Fixture");
    }
    
}
