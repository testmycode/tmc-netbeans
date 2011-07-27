package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.utilities.http.NetworkTasks;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONCourseListParser;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONExerciseListParser;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONSubmissionResultParser;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectUnzipper;
import fi.helsinki.cs.tmc.utilities.zip.NbProjectZipper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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
            defaultInstance = new ServerAccess(
                    new NetworkTasks(),
                    ProjectMediator.getInstance(),
                    NbProjectUnzipper.getDefault(),
                    NbProjectZipper.getDefault()
                    );
        }
        return defaultInstance;
    }
    
    private static Preferences getPreferences() {
        return NbPreferences.forModule(ServerAccess.class);
    }
    
    private NetworkTasks networkTasks;
    private ProjectMediator projectMediator;
    private NbProjectUnzipper unzipper;
    private NbProjectZipper zipper;
    private String baseUrl;
    private String username;

    public ServerAccess(
            NetworkTasks networkTasks,
            ProjectMediator projectMediator,
            NbProjectUnzipper unzipper,
            NbProjectZipper zipper) {
        this.networkTasks = networkTasks;
        this.projectMediator = projectMediator;
        this.unzipper = unzipper;
        this.zipper = zipper;
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

    public NetworkTasks getNetworkTasks() {
        return networkTasks;
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
        CancellableCallable<ExerciseCollection> task = new CancellableCallable<ExerciseCollection>() {
            @Override
            public ExerciseCollection call() throws Exception {
                String json = download.call();
                ExerciseCollection exercises = JSONExerciseListParser.parseJson(json);
                exercises.setCourseNameForEach(course.getName());
                return exercises;
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
        
        return new BgTask("Download " + listUrl, listener, task).start();
    }

    
    public Future<TmcProjectInfo> startDownloadingExerciseProject(final Exercise exercise, BgTaskListener<TmcProjectInfo> listener) {
        final String zipUrl = exercise.getDownloadAddress();
        
        final CancellableCallable<byte[]> download = networkTasks.downloadBinaryFile(zipUrl);
        CancellableCallable<TmcProjectInfo> task = new CancellableCallable<TmcProjectInfo>() {
            @Override
            public TmcProjectInfo call() throws Exception {
                byte[] data = download.call();
                File courseDir = projectMediator.getCourseRootDir(exercise.getCourseName());
                unzipper.unzipProject(data, courseDir, exercise.getName());
                TmcProjectInfo project = projectMediator.tryGetProjectForExercise(exercise);
                if (project == null) {
                    throw new Exception("Failed to open the downloaded project");
                }
                return project;
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
        
        return new BgTask("Download " + zipUrl, listener, task).start();
    }
    
    public Future<SubmissionResult> startSubmittingExercise(final Exercise exercise, BgTaskListener<SubmissionResult> listener) {
        final String submitUrl = exercise.getReturnAddress();
        
        File projectDir = projectMediator.getProjectDirForExercise(exercise);
        
        final byte[] file;
        try {
            file = zipper.zipProjectSources(projectDir.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to zip up exercise", e);
        }
        
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("submission[username]", this.getUsername());
        
        final CancellableCallable<String> upload =
                networkTasks.uploadFileForTextResponse(submitUrl, params, "submission[file]", file);
        
        CancellableCallable<SubmissionResult> task = new CancellableCallable<SubmissionResult>() {
            @Override
            public SubmissionResult call() throws Exception {
                String json = upload.call();
                return JSONSubmissionResultParser.parseJson(json);
            }

            @Override
            public boolean cancel() {
                return upload.cancel();
            }
        };
        
        return new BgTask("Upload " + exercise.getName(), listener, task).start();
    }
}
