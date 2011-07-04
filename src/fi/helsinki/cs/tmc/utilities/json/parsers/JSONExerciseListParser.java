/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.utilities.json.parsers;

import java.text.SimpleDateFormat;
import java.util.Date;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import fi.helsinki.cs.tmc.utilities.json.parsers.jsonorg.JSONArray;
import fi.helsinki.cs.tmc.utilities.json.parsers.jsonorg.JSONException;
import fi.helsinki.cs.tmc.utilities.json.parsers.jsonorg.JSONObject;

/**
 *
 * @author jmturpei
 */
public class JSONExerciseListParser {

    /**
     * Method parses an ExerciseCollection from json and course parameters 
     * @param json String 
     * @param course Course
     * @return exerciseCollection ExerciseCollection
     * @throws JSONException
     * @throws NullPointerException 
     */
    public static ExerciseCollection parseJson(String json, Course course) throws JSONException, NullPointerException {
        if (json == null) {
            throw new NullPointerException("Json string is null");
        }

        ExerciseCollection exerciseCollection = new ExerciseCollection(course);

        JSONArray jsonExerciseCollection = new JSONArray(json);

        for (int i = 0; i < jsonExerciseCollection.length(); i++) {

            JSONObject jsonExercise = jsonExerciseCollection.getJSONObject(i).getJSONObject("exercise");

            Exercise exercise = createExercise(jsonExercise);

            exerciseCollection.add(exercise);

        }

        return exerciseCollection;
    }

    /**
     * Method checks if String json parameter is in proper json form 
     * @param json String
     * @param course Course
     * @return true or false boolean
     */
    public static boolean isJsonValid(String json, Course course) {
        try {
            parseJson(json, course);
        } catch (JSONException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
        return true;
    }

    /**
     * Creates a Exercise object from JSONObject parameter.  
     * @param jsonExercise JSONObject
     * @return exercise Exercise
     */
    private static Exercise createExercise(JSONObject jsonExercise) throws JSONException {
        Exercise exercise = new Exercise();

        try {

            exercise.setName(jsonExercise.getString("name"));
            exercise.setDownloadAddress(jsonExercise.getString("exercise_file"));
            exercise.setReturnAddress(jsonExercise.getString("return_address"));

        } catch (JSONException e) {
            throw new JSONException("Invalid JSONObject!");
        }

        exercise.setDeadline(parseDeadline(jsonExercise.getString("deadline")));

        return exercise;
    }

    /**
     * Method parses a Date deadline from String deadline parameter
     * @param deadline String
     * @return deadlineDate Date
     * @throws JSONException 
     */
    private static Date parseDeadline(String deadline) throws JSONException {

        //2011-06-08T16:19:15+03:00

        Date deadlineDate = null;

        try {

            SimpleDateFormat dateTimeParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

            deadlineDate = dateTimeParser.parse(deadline);
        } catch (Exception e) {
            throw new JSONException("invalid deadline format in exercise list");
        }

        return deadlineDate;

    }
}
