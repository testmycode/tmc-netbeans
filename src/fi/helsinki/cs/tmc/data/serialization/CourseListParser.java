package fi.helsinki.cs.tmc.data.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.data.Exercise;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CourseListParser {
    
    private static class CourseListContainer {
        public int apiVersion;
        public Course[] courses;
    }
    
    /**
     * Creates a CourseList object from text.
     */
    public CourseList parseFromJson(String json) {
        if (json == null) {
            throw new NullPointerException("Json string is null");
        }
        if (json.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty input");
        }
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Date.class, new CustomDateDeserializer())
                    .create();
            
            Course[] courses = gson.fromJson(json, CourseListContainer.class).courses;

            CourseList courseList = new CourseList();
            for (Course course : courses) {
                courseList.add(course);
                for (Exercise ex : course.getExercises()) {
                    ex.setCourseName(course.getName());
                }
            }

            return courseList;
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
