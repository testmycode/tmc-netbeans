package fi.helsinki.cs.tmc.settings;

import java.io.IOException;
import java.io.InputStream;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import fi.helsinki.cs.tmc.utilities.ModalDialogDisplayer;

/**
 * This class is used to load and save settings. The actual work is done in
 * Settings object.
 * @author jmturpei
 */
public class LegacyPluginSettings {

    private static LegacySettings settings;

    /**
     * Constructor
     */
    private LegacyPluginSettings() {
    }

    static {
        try {
            loadFromFile();
        } catch (Exception e) {
            ModalDialogDisplayer.getDefault().displayError("Error in Class:PluginSettings.java Method:loadFromFile Message: " + e);
        }
    }

    public static LegacySettings getSettings() {
        return settings;
    }

    /**
     * Method saves user's plugin settings
     * @param settings 
     */
    public static void saveSettings() throws Exception {
        FileObject folder;
        FileObject file;

        if ((folder = FileUtil.getConfigFile("/Palikka/")) == null) {
            try {
                folder = FileUtil.getConfigRoot().createFolder("Palikka");
            } catch (IOException e) {
                throw new IOException("Failed to save settings. (Unable to create a folder into the Netbeans' filesystem!)");
            }
        }

        if ((file = folder.getFileObject("Settings", "xml")) == null) {
            try {
                file = folder.createData("Settings", "xml");
            } catch (IOException e) {
                throw new IOException("Failed to save settings. (Unable to write settings file into the Netbean's filesystem!)");
            }
        }

        LegacySettings.Serialize(settings, file.getOutputStream());
    }

    /**
     * Method loads user's plugin settings from file.
     */
    public static void loadFromFile() throws Exception {

        InputStream xml = getXmlSettingFile();

        settings = LegacySettings.Deserialize(xml);
    }

    /**
     * Find the settings xml file from disk.
     * @return InputStream to the xml
     * @throws Exception When it fails to load settings.
     */
    private static InputStream getXmlSettingFile() throws Exception {
        FileObject settingsFile;
        InputStream xml;
        if ((settingsFile = FileUtil.getConfigFile("/Palikka/Settings.xml")) == null) {

            xml = Lookup.getDefault().lookup(ClassLoader.class).getResourceAsStream("palikka/defaultSettings.xml");



        } else {
            xml = settingsFile.getInputStream();

        }

        return xml;
    }
}
