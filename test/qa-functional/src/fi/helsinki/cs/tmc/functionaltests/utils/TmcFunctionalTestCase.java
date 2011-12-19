package fi.helsinki.cs.tmc.functionaltests.utils;

import java.io.File;
import javax.swing.JDialog;
import org.netbeans.jellytools.JellyTestCase;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jemmy.JemmyException;
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
        dismissInitialDialogIfAny();
    }
    
    @Override
    protected void tearDown() throws Exception {
        this.clearWorkDir();
        serverFixture.tearDown();
        super.tearDown();
    }
    
    private void dismissInitialDialogIfAny() {
        try {
            new NbDialogOperator("TMC installed").ok();
            JDialog settingsDialog = JDialogOperator.waitJDialog("TMC Settings", true, true);
            JButtonOperator.findJButton(settingsDialog, "Cancel", true, true).doClick();
        } catch (JemmyException e) {
        }
    }
    
    protected void arrangeForCourseToBeDownloaded(String courseName) throws Exception {
        serverFixture.addDefaultCourse(courseName, getTestProjectZip());
        SettingsOperator.setAllSettings(this, courseName);
        
        new NbDialogOperator("Open exercises?").btYes().doClick();
    }
    
    protected File getTestProjectZip() {
        return new File(getDataDir().getPath() + File.separator + "TestProject.zip");
    }
    
}
