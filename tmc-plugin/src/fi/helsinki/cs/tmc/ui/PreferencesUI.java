package fi.helsinki.cs.tmc.ui;


import fi.helsinki.cs.tmc.core.domain.Course;

import java.util.List;
import java.util.Locale;

public interface PreferencesUI {

    String getProjectDir();

    String getSelectedCourseName();

    String getServerBaseUrl();

    String getUsername();
    
    String getPassword();
    
    boolean getShouldSavePassword();

    public List<Course> getAvailableCourses();
    
    boolean getCheckForUpdatesInTheBackground();
    
    boolean getCheckForUnopenedExercisesAtStartup();
    
    boolean getSpywareEnabled();
    
    Locale getErrorMsgLocale();

    void setAvailableCourses(List<Course> courses);
    
    void setProjectDir(String projectDir);

    void setSelectedCourseName(String courseName);

    void setServerBaseUrl(String baseUrl);

    void setUsername(String username);
    
    void setUsernameFieldName(String usernameFieldName);
    
    void setPassword(String password);
    
    void setShouldSavePassword(boolean shouldSavePassword);
    
    void setCheckForUpdatesInTheBackground(boolean shouldCheck);
    
    void setCheckForUnopenedExercisesAtStartup(boolean shouldCheck);
    
    void setSpywareEnabled(boolean enabled);
    
    void setErrorMsgLocale(Locale locale);
}
