package fi.helsinki.cs.tmc.functionaltests.utils;

import fi.helsinki.cs.tmc.functionaltests.utils.FakeTmcServer;
import javax.swing.JDialog;
import org.netbeans.jellytools.JellyTestCase;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JDialogOperator;

public abstract class TmcFunctionalTestCase extends JellyTestCase {

    protected FakeTmcServer fakeServer;
    
    public TmcFunctionalTestCase(String testName) {
        super(testName);
        closeAllModal = true;
    }

    @Override
    protected void setUp() throws Exception {
        fakeServer = new FakeTmcServer();
        fakeServer.start();
        super.setUp();
        dismissInitialDialogIfAny();
    }
    
    @Override
    protected void tearDown() throws Exception {
        fakeServer.stop();
        fakeServer = null;
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
    
}
