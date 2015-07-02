package fi.helsinki.cs.tmc.data.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hy.tmc.core.domain.Course;
import hy.tmc.core.domain.Exercise;
import java.util.Date;

public class CourseInfoParser {
    private static class CourseInfoContainer {
        public int apiVersion;
        public Course course;
    }

    public Course parseFromJson(String json) {
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

            Course course = gson.fromJson(json, CourseInfoContainer.class).course;

            course.setExercisesLoaded(true);
            
            for (Exercise ex : course.getExercises()) {
                ex.setCourseName(course.getName());
            }

            return course;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to parse course list: " + e.getMessage(), e);
        }
    }
}
