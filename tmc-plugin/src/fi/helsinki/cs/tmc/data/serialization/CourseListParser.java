package fi.helsinki.cs.tmc.data.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CourseListParser {

    private static class CourseListContainer {
        public int apiVersion;
        public Course[] courses;
    }

    public List<Course> parseFromJson(String json) {
        if (json == null) {
            throw new NullPointerException("Json string is null");
        }
        if (json.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty input");
        }
        try {
            Gson gson =
                    new GsonBuilder()
                            .registerTypeAdapter(Date.class, new CustomDateDeserializer()).create();

            Course[] courses = gson.fromJson(json, CourseListContainer.class).courses;

            List<Course> courseList = new ArrayList<Course>();
            for (Course course : courses) {
                courseList.add(course);
                course.setExercisesLoaded(false);
                for (Exercise ex : course.getExercises()) {
                    ex.setCourseName(course.getName());
                }
            }

            return courseList;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to parse course list: " + e.getMessage(), e);
        }
    }
}
