package fi.helsinki.cs.tmc.application;

import org.openide.modules.ModuleInstall;
import org.openide.modules.Modules;
import fi.helsinki.cs.tmc.api.TmcPlugin;

public class TmcAppModuleInstall extends ModuleInstall {

    @Override
    public void restored() {
        setApplicationBuildNumberToTmcModuleVersion();
    }

    private void setApplicationBuildNumberToTmcModuleVersion() {
        // This version number will be shown in the title bar.
        // See http://netbeansrcp.wordpress.com/2009/04/15/change-the-version-number-in-a-netbeans-platform-application/
        String version =
                Modules.getDefault().ownerOf(TmcPlugin.class).getSpecificationVersion().toString();
        System.setProperty("netbeans.buildnumber", version);
    }
}
