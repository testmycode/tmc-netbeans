package fi.helsinki.cs.tmc.data;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Course {

    private String name;
    
    @SerializedName("unlock_url")
    private String unlockUrl;
    @SerializedName("reviews_url")
    private String reviewsUrl;
    @SerializedName("comet_url")
    private String cometUrl;
    
    private List<Exercise> exercises;
    
    private List<String> unlockables; // Exercise names
    
    
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
        this.name = name;
    }
    
    public String getUnlockUrl() {
        return unlockUrl;
    }

    public void setUnlockUrl(String unlockUrl) {
        this.unlockUrl = unlockUrl;
    }

    public String getReviewsUrl() {
        return reviewsUrl;
    }

    public void setReviewsUrl(String reviewsUrl) {
        this.reviewsUrl = reviewsUrl;
    }

    public String getCometUrl() {
        return cometUrl;
    }

    public void setCometUrl(String cometUrl) {
        this.cometUrl = cometUrl;
    }

    public List<String> getUnlockables() {
        return unlockables;
    }

    public void setUnlockables(List<String> unlockables) {
        this.unlockables = unlockables;
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
