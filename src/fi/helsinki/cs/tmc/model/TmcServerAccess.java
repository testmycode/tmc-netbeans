package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.Refactored;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import fi.helsinki.cs.tmc.utilities.http.FileDownloader;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONCourseListParser;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONExerciseListParser;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 * A frontend for the server.
 */
@Refactored
public class TmcServerAccess {
    private static final String PREF_BASE_URL = "baseUrl";
    private static final String PREF_USERNAME = "username";
    
    private static TmcServerAccess defaultInstance;
    
    public static TmcServerAccess getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new TmcServerAccess(new FileDownloader());
        }
        return defaultInstance;
    }
    
    private static Preferences getPreferences() {
        return NbPreferences.forModule(TmcServerAccess.class);
    }
    
    private FileDownloader fileDownloader;
    private String baseUrl;
    private String username;

    
    public TmcServerAccess(FileDownloader fileDownloader) {
        this.fileDownloader = fileDownloader;
        loadPreferences();
    }
    
    private void loadPreferences() {
        Preferences prefs = getPreferences();
        this.baseUrl = prefs.get(PREF_BASE_URL, "");
        this.username = prefs.get(PREF_USERNAME, "");
    }

    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        getPreferences().put(PREF_BASE_URL, baseUrl);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        getPreferences().put(PREF_USERNAME, username);
    }
    
    public Future<CourseCollection> startDownloadingCourseList(BgTaskListener<CourseCollection> listener) {
        Callable<CourseCollection> task = new Callable<CourseCollection>() {
            @Override
            public CourseCollection call() throws Exception {
                String json = fileDownloader.downloadTextFile(getCourseListUrl());
                return JSONCourseListParser.parseJson(json);
            }
        };
        return BgTask.start("Download " + getCourseListUrl(), listener, task);
    }
    
    private String getCourseListUrl() {
        return baseUrl + "/courses.json";
    }

    public Future<ExerciseCollection> startDownloadingExerciseList(final Course course, BgTaskListener<ExerciseCollection> listener) {
        final String listUrl = course.getExerciseListDownloadAddress();
        Callable<ExerciseCollection> task = new Callable<ExerciseCollection>() {
            @Override
            public ExerciseCollection call() throws Exception {
                String json = fileDownloader.downloadTextFile(listUrl);
                return JSONExerciseListParser.parseJson(json);
            }
        };
        return BgTask.start("Download " + listUrl, listener, task);
    }
    
}
