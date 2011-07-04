package palikka.utilities.json.parsers;

import java.util.ArrayList;
import palikka.utilities.json.parsers.jsonorg.JSONArray;
import palikka.utilities.json.parsers.jsonorg.JSONException;
import palikka.utilities.json.parsers.jsonorg.JSONObject;

/**
 * A prototype for parsing test results. At the time of creation hidden tests
 * were not implemented.
 * @author knordman
 */
public class JSONTestResultsParser {

    /**
     * Method parses the exercise's tests failure results from json String and returns 
     * them in a failures ArrayList 
     * @param json String
     * @return failure ArrayList<String> 
     * @throws JSONException
     * @throws NullPointerException 
     */
    public static ArrayList<String> parseJson(String json) throws JSONException, NullPointerException {
        ArrayList<String> failures = new ArrayList<String>();

        try {
            JSONArray jsonResults = new JSONArray(json);

            for (int i = 0; i < jsonResults.length(); i++) {

                JSONObject jsonResult = jsonResults.getJSONObject(i).getJSONObject("test_case_run");
                if (!jsonResult.getBoolean("success")) {
                    failures.add(createFailMessage(jsonResult));
                }
            }
        } catch (JSONException e) {
            throw new JSONException("invalid JSON String!");
        }
        return failures;
    }

    /**
     * Method checks if String json parameter is in proper json form 
     * @param json
     * @return true or false
     */
    public static boolean isValidJson(String json) {
        try {
            parseJson(json);
        } catch (JSONException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
        return true;
    }

    /**
     * Method creates a message which tells user how exercise's tests has failed
     * @param result
     * @return message 
     * @throws JSONException
     * @throws NullPointerException 
     */
    private static String createFailMessage(JSONObject result) throws JSONException, NullPointerException {
        String message = "Test failed for exercise " + result.getString("exercise") + ". Cause: " + result.getString("message");

        return message;
    }
}
