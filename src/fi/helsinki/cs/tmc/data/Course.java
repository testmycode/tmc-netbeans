package fi.helsinki.cs.tmc.data;

import com.google.gson.annotations.SerializedName;

public class Course {

    private String name;
    
    @SerializedName("exercises_json")
    private String exerciseListDownloadAddress;
    
    
    public Course() {
    }
    
    /**
     * Returns name of Course object
     * @return this.name
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Sets Course objects name
     * @param name 
     */
    public void setName(String name) {
        if(name == null) throw new NullPointerException("name was null at Course.setName");
        if(name.isEmpty()) throw new IllegalArgumentException("Name cannot be empty at Course.setName");
        
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    /**
     * @return the exerciseListDownloadAddress
     */
    public String getExerciseListDownloadAddress() {
        return exerciseListDownloadAddress;
    }
}
