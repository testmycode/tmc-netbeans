package palikka.utilities.textio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import palikka.settings.PluginSettings;

/**
 * This class is used to read files from disk to String objects.
 * More specifically JSON files.
 * @author kkaltiai
 */
public class ReadFromFile {

    /**
     * Constructor
     */
    public ReadFromFile() {
    }

    /**
     * Tries to read json file from default folder and returns it transformed into a String.
     * @param filename Name of JSON file.
     * @return String Representation of the JSON file or null if there is no file available.
     * @throws IOException
     */
    public String readFromFile(String filename) throws IOException {
        String path = PluginSettings.getSettings().getDefaultFolder() + "/" + filename;

        File jsonFile = new File(path);

        if (jsonFile.exists()) {
            try {

                BufferedReader in = new BufferedReader(new FileReader(path));
                String jsonString = "", line;

                while ((line = in.readLine()) != null) {
                    jsonString += line;
                }

                in.close();

                return jsonString;
            } catch (Exception e) {
                throw new IOException("Cannot open file: " + jsonFile.getAbsolutePath());
            }
        }
        return null;
    }
}
