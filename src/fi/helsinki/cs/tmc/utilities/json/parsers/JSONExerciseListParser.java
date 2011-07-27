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
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;


public class JSONExerciseListParser {

    /**
     * Parses a collection of exercises and attaches them to a course.
     */
    public static ExerciseCollection parseJson(String json) {
        try {
            if (json == null) {
                throw new NullPointerException("Json string is null");
            }

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Date.class, new CustomDateDeserializer())
                    .create();
            Exercise[] exercises = gson.fromJson(json, Exercise[].class);

            ExerciseCollection exerciseCollection = new ExerciseCollection();
            for (Exercise exercise : exercises) {
                exerciseCollection.add(exercise);
            }

            return exerciseCollection;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to parse exercise list: " + e.getMessage(), e);
        }
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
