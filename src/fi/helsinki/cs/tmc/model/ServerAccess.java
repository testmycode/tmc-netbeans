package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import fi.helsinki.cs.tmc.utilities.http.NetworkTasks;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONCourseListParser;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONExerciseListParser;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 * A frontend for the server.
 */
public class ServerAccess {
    private static final String PREF_BASE_URL = "baseUrl";
    private static final String PREF_USERNAME = "username";
    
    private static ServerAccess defaultInstance;
    
    public static ServerAccess getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new ServerAccess(new NetworkTasks());
        }
        return defaultInstance;
    }
    
    private static Preferences getPreferences() {
        return NbPreferences.forModule(ServerAccess.class);
    }
    
    private NetworkTasks networkTasks;
    private String baseUrl;
    private String username;

    
    public ServerAccess(NetworkTasks networkTasks) {
        this.networkTasks = networkTasks;
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
        baseUrl = stripTrailingSlashes(baseUrl);
        this.baseUrl = baseUrl;
        getPreferences().put(PREF_BASE_URL, baseUrl);
    }
    
    private String getCourseListUrl() {
        return baseUrl + "/courses.json";
    }
    
    private String stripTrailingSlashes(String s) {
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        getPreferences().put(PREF_USERNAME, username);
    }
    
    public Future<CourseCollection> startDownloadingCourseList(BgTaskListener<CourseCollection> listener) {
        final CancellableCallable<String> download = networkTasks.downloadTextFile(getCourseListUrl());
        CancellableCallable<CourseCollection> task = new CancellableCallable<CourseCollection>() {
            @Override
            public CourseCollection call() throws Exception {
                String json = download.call();
                return JSONCourseListParser.parseJson(json);
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
        
        return new BgTask("Download " + getCourseListUrl(), listener, task).start();
    }

    public Future<ExerciseCollection> startDownloadingExerciseList(final Course course, BgTaskListener<ExerciseCollection> listener) {
        final String listUrl = course.getExerciseListDownloadAddress();
        
        final CancellableCallable<String> download = networkTasks.downloadTextFile(listUrl);
        Callable<ExerciseCollection> task = new Callable<ExerciseCollection>() {
            @Override
            public ExerciseCollection call() throws Exception {
                String json = download.call();
                ExerciseCollection exercises = JSONExerciseListParser.parseJson(json);
                exercises.setCourseNameForEach(course.getName());
                return exercises;
            }
        };
        
        return new BgTask("Download " + listUrl, listener, task).start();
    }
    
}
