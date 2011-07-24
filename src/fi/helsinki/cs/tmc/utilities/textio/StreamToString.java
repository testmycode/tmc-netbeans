package fi.helsinki.cs.tmc.utilities.textio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

/**
 * This class is used to read InputStreams to String objects.
 * @author kkaltiai
 */
@Deprecated // use commons IO instead
public class StreamToString {

    /**
     * Transforms InputStream 'in' parameter into String.
     * @param in Input stream to be read.
     * @return Returns String that has been transformed from 'in' parameter.
     * @throws Exception
     */
    public static String inputStreamToString(InputStream in) throws Exception {
        StringWriter writer = new StringWriter();
        char[] buffer = new char[1024];

        if (in != null) {
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(in, "UTF-8"));  //InputStreamReader converts InputStreams to character streams

                int len;
                while ((len = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, len);
                }
            } catch (IOException ioex) {
                throw new Exception("Failed to process json");
            } finally {
                in.close();
            }
            return writer.toString();
        } else {
            throw new Exception("Failed to process json.");
        }
    }
}
