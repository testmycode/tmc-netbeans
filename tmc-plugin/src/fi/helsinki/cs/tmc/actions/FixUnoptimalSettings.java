package fi.helsinki.cs.tmc.actions;

import org.openide.util.NbPreferences;

import java.util.prefs.Preferences;

public class FixUnoptimalSettings {

    private final Preferences mavenPrefrences;

    public FixUnoptimalSettings() {
        this.mavenPrefrences = NbPreferences.root()
                .node("org")
                .node("netbeans")
                .node("modules")
                .node("maven");
    }

    public void run() {
        fixMavenDependencyDownloadPolicy();
    }

    private void fixMavenDependencyDownloadPolicy() {
        final String binaryDownloadValue = mavenPrefrences.get("binaryDownload", "");
        if (!binaryDownloadValue.equals("EVERY_OPEN")) {
            mavenPrefrences.put("binaryDownload", "EVERY_OPEN");
        }
    }

    void undo() {
        final String binaryDownloadValue = mavenPrefrences.get("binaryDownload", "");
        if (binaryDownloadValue.equals("EVERY_OPEN")) {
            mavenPrefrences.put("binaryDownload", "NEVER");
        }
    }
}
