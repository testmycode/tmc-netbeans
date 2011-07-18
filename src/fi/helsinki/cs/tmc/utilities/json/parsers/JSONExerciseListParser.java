/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.helsinki.cs.tmc.utilities.json.parsers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
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
     */
    public static ExerciseCollection parseJson(String json, Course course) {
        if (json == null) {
            throw new NullPointerException("Json string is null");
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new CustomDateDeserializer())
                .create();
        Exercise[] exercises = gson.fromJson(json, Exercise[].class);

        ExerciseCollection exerciseCollection = new ExerciseCollection(course);
        for (Exercise exercise : exercises) {
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
    @Deprecated
    public static boolean isJsonValid(String json, Course course) {
        try {
            parseJson(json, course);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    private static class CustomDateDeserializer implements JsonDeserializer<Date> {
        @Override
        public Date deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
            SimpleDateFormat dateTimeParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            try {
                return dateTimeParser.parse(je.getAsString());
            } catch (ParseException ex) {
                throw new JsonParseException(ex);
            }
        }
    }
}
