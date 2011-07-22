package fi.helsinki.cs.tmc.data;

import com.google.gson.annotations.SerializedName;
import fi.helsinki.cs.tmc.Refactored;

@Refactored
public class Course {

    private String name;
    
    @SerializedName("exercises_json")
    private String exerciseListDownloadAddress;
    
    
    public Course() {
    }

    public Course(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        if(name == null) throw new NullPointerException("name was null at Course.setName");
        if(name.isEmpty()) throw new IllegalArgumentException("Name cannot be empty at Course.setName");
        
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    public String getExerciseListDownloadAddress() {
        return exerciseListDownloadAddress;
    }

    public void setExerciseListDownloadAddress(String exerciseListDownloadAddress) {
        this.exerciseListDownloadAddress = exerciseListDownloadAddress;
    }
}
