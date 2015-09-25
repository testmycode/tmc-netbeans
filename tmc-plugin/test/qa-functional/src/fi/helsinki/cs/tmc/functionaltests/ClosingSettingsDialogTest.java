package fi.helsinki.cs.tmc.functionaltests;

import static fi.helsinki.cs.tmc.functionaltests.utils.TmcFunctionalTestCase.loadSuite;

import fi.helsinki.cs.tmc.functionaltests.utils.SettingsOperator;
import fi.helsinki.cs.tmc.functionaltests.utils.TmcFunctionalTestCase;

import junit.framework.Test;

public class ClosingSettingsDialogTest extends TmcFunctionalTestCase{
    public static Test suite() {
        return loadSuite(ClosingSettingsDialogTest.class);
    }

    public ClosingSettingsDialogTest() {
        super("ClosingSettingsDialogTest");
    }

    public void testClosingSettingsDialog() throws Exception {
        SettingsOperator settings = SettingsOperator.openSettingsDialog();
        settings.closeDialogWindow();
        settings = SettingsOperator.openSettingsDialog();
    }
}
