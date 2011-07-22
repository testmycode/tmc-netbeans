package fi.helsinki.cs.tmc.utilities.textio;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import fi.helsinki.cs.tmc.settings.LegacyPluginSettings;
import fi.helsinki.cs.tmc.utilities.PalikkaConstants;

/**
 * This class is used to read Strings to files.
 * Palikka uses it only for JSON files.
 * @author kkaltiai
 */
public class WriteToFile {

    /**
     * Constructor
     */
    public WriteToFile() {
    }

    /**
     * Saves the current JSONString into a file under the default folder set by the user.
     * If the user has not set a default folder, this method doesn't do anything.
     * @param json JSON string.
     * @param filename File where JSON string is written.
     * @throws Exception When the method fails to write the file to disk.
     */
    public void writeToFile(String json, String filename) throws Exception {
        String defaultFolder = LegacyPluginSettings.getSettings().getDefaultFolder();
        if (defaultFolder == null) {
            return;
        }

        try {

            FileWriter outFile = new FileWriter(defaultFolder + PalikkaConstants.fileSeparator + filename);
            PrintWriter out = new PrintWriter(outFile);

            out.print(json);

            out.close();
            outFile.close();

        } catch (Exception e) {
            throw new IOException("Unable to write file " + defaultFolder + PalikkaConstants.fileSeparator + filename);
        }
    }
}
