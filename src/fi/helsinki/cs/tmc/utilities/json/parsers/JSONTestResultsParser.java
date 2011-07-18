package fi.helsinki.cs.tmc.utilities.json.parsers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;

/**
 * A prototype for parsing test results. At the time of creation hidden tests
 * were not implemented.
 * @author knordman
 */
public class JSONTestResultsParser {

    /**
     * Method parses the exercise's tests failure results from json String and returns 
     * them in a failures ArrayList 
     */
    public static ArrayList<String> parseJson(String json) {
        ArrayList<String> failures = new ArrayList<String>();

        Gson gson = new Gson();
        JsonArray jsonResults = gson.fromJson(json, JsonArray.class);

        for (int i = 0; i < jsonResults.size(); i++) {

            JsonObject jsonResult = jsonResults.get(i).getAsJsonObject().getAsJsonObject("test_case_run");
            if (!jsonResult.get("success").getAsBoolean()) {
                failures.add(createFailMessage(jsonResult));
            }
        }
        return failures;
    }

    /**
     * Method checks if String json parameter is in proper json form 
     * @param json
     * @return true or false
     */
    @Deprecated
    public static boolean isValidJson(String json) {
        try {
            parseJson(json);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Method creates a message which tells user how exercise's tests has failed
     * @param result
     * @return message
     */
    private static String createFailMessage(JsonObject result) {
        String message = "Test failed for exercise " + result.get("exercise").getAsString() + ". Cause: " + result.get("message").getAsString();

        return message;
    }
}
