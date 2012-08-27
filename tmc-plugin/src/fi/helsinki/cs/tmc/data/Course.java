package fi.helsinki.cs.tmc.data;

import java.util.ArrayList;
import java.util.List;

public class Course {

    private String name;
    
    private List<Exercise> exercises;
    
    
    public Course() {
        this(null);
    }

    public Course(String name) {
        this.name = name;
        this.exercises = new ArrayList<Exercise>();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        if(name == null) throw new NullPointerException("name was null at Course.setName");
        if(name.isEmpty()) throw new IllegalArgumentException("Name cannot be empty at Course.setName");
        
        this.name = name;
    }

    public List<Exercise> getExercises() {
        return exercises;
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
    }
    
    @Override
    public String toString() {
        //TODO: this cannot return anything else until PreferencesPanel is fixed to not use toString to present Course objects
        return name;
    }
}
