package fi.helsinki.cs.tmc.actions;

import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import org.openide.util.NbPreferences;

import java.util.prefs.Preferences;
import org.openide.util.Exceptions;

public class FixUnoptimalSettings {

    private final Preferences mavenPrefrences;
    private final Preferences indexingPreferences;

    public FixUnoptimalSettings() {
        this.mavenPrefrences = NbPreferences.root()
                .node("org")
                .node("netbeans")
                .node("modules")
                .node("maven");
        this.indexingPreferences = NbPreferences.root()
                .node("org")
                .node("netbeans")
                .node("modules")
                .node("maven")
                .node("nexus")
                .node("indexing");
    }

    public void run() {
        fixMavenDependencyDownloadPolicy();
        fixMavenIndexingPolicy();
    }

    private void fixMavenDependencyDownloadPolicy() {
        final String binaryDownloadValue = mavenPrefrences.get("binaryDownload", "");
        if (!binaryDownloadValue.equals("EVERY_OPEN")) {
            mavenPrefrences.put("binaryDownload", "EVERY_OPEN");
        }
    }
    
    private void fixMavenIndexingPolicy() {
        final String disableIndexingValue = indexingPreferences.get("createIndex", "");
        if (!disableIndexingValue.equals("false")) {
            indexingPreferences.put("createIndex", "false");
        }
        final String updateFrequencyValue = indexingPreferences.get("indexUpdateFrequency", "");
        if (!updateFrequencyValue.equals("3")) {
            indexingPreferences.put("indexUpdateFrequency", "3");
        }
    }
}
