package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseList;

public interface PreferencesUI {

    String getProjectDir();

    Course getSelectedCourse();

    String getServerBaseUrl();

    String getUsername();

    void setAvailableCourses(CourseList courses);

    void courseRefreshFailedOrCanceled();
    
    void setProjectDir(String projectDir);

    void setSelectedCourse(Course course);

    void setServerBaseUrl(String baseUrl);

    void setUsername(String username);
    
}
