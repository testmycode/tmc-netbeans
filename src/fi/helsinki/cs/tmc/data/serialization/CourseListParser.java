package fi.helsinki.cs.tmc.data.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.data.Exercise;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CourseListParser {
    
    /**
     * Creates a CourseCollection object from text.
     */
    public CourseCollection parseFromJson(String json) {
        if (json == null) {
            throw new NullPointerException("Json string is null");
        }
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Date.class, new CustomDateDeserializer())
                    .create();
            Course[] courses = gson.fromJson(json, Course[].class);

            CourseCollection courseCollection = new CourseCollection();
            for (Course course : courses) {
                courseCollection.add(course);
                for (Exercise ex : course.getExercises()) {
                    ex.setCourseName(course.getName());
                }
            }

            return courseCollection;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to parse course list: " + e.getMessage(), e);
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
