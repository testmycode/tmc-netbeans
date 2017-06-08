package fi.helsinki.cs.tmc.timeline;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.Skill;
import fi.helsinki.cs.tmc.model.CourseDb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.TreeMap;

public class TimelineJson {

    private Map<String, String> colors;
    private CourseDb courseDb;
    private int week;

    private static final Logger logger = Logger.getLogger(TimelineJson.class.getName());

    public TimelineJson(int week) {
        courseDb = CourseDb.getInstance();
        this.week = week;
        initializeColors();
    }

    private void initializeColors() {
        colors = new HashMap<>();
        colors.put("black", "#000");
        colors.put("red", "#f00");
        colors.put("yellow", "#ff0");
        colors.put("green", "#0f0");
    }

    public String getExerciseColorJsonMap() {
        List<Exercise> exercises = courseDb.getExercisesByWeek(week);
        TreeMap<String, String> exerciseMap = new TreeMap<>();
        for (Exercise ex : exercises) {
            if (!ex.isAdaptive()) {
                String color = getExerciseColor(ex);
                exerciseMap.put(ex.getName(), color);
            }
        }
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(exerciseMap);

        return json;
    }

    public String getSkillColorJsonMap() {
        List<Skill> skills = courseDb.getSkillsByWeek(week);
        TreeMap<String, String> skillMap = new TreeMap<>();
        for (Skill skill : skills) {
            String color = getSkillColor(skill);
            skillMap.put(skill.getName(), color);
        }
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(skillMap);

        return json;
    }

    private String getExerciseColor(Exercise ex) {
        if (ex.isCompleted()) {
            return colors.get("green");
        }
        return ex.isAttempted() ? colors.get("red") : colors.get("black");
    }

    private String getSkillColor(Skill skill) {
        if (skill.getPercentage() > 1.0) {
            return colors.get("black");
        }
        return skill.isMastered() ? colors.get("green") : colors.get("red");
    }
}
