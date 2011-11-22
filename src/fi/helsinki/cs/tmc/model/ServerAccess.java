package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.utilities.http.HttpTasks;
import fi.helsinki.cs.tmc.data.serialization.CourseListParser;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.data.serialization.SubmissionResultParser;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * A frontend for the server.
 */
public class ServerAccess {
    private static final int API_VERSION = 1;
    
    public static ServerAccess create() {
        return new ServerAccess(
                TmcSettings.getSaved(),
                new HttpTasks(),
                new CourseListParser(),
                new SubmissionResultParser()
                );
    }
    
    private TmcSettings settings;
    private HttpTasks networkTasks;
    private CourseListParser courseListParser;
    private SubmissionResultParser submissionResultParser;

    public ServerAccess(
            TmcSettings settings,
            HttpTasks networkTasks,
            CourseListParser courseListParser,
            SubmissionResultParser submissionResultParser) {
        this.settings = settings;
        this.networkTasks = networkTasks;
        this.courseListParser = courseListParser;
        this.submissionResultParser = submissionResultParser;
    }
    
    public void setSettings(TmcSettings settings) {
        this.settings = settings;
    }
    
    public HttpTasks getNetworkTasks() {
        return networkTasks;
    }
    
    private String getCourseListUrl() {
        String query =
                "api_version=" + API_VERSION +
                "&api_username=" + encParam(settings.getUsername()) +
                "&api_password=" + encParam(settings.getPassword());
        return settings.getServerBaseUrl() + "/courses.json?" + query;
    }
    
    private String encParam(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public boolean hasEnoughSettings() {
        return
                !settings.getUsername().isEmpty() &&
                !settings.getPassword().isEmpty() &&
                !settings.getServerBaseUrl().isEmpty();
    }
    
    public Future<CourseList> startDownloadingCourseList(BgTaskListener<CourseList> listener) {
        final CancellableCallable<String> download = networkTasks.downloadTextFile(getCourseListUrl());
        CancellableCallable<CourseList> task = new CancellableCallable<CourseList>() {
            @Override
            public CourseList call() throws Exception {
                String text = download.call();
                return courseListParser.parseFromJson(text);
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
        
        return BgTask.start("Downloading " + getCourseListUrl(), listener, task);
    }
    
    public Future<byte[]> startDownloadingExerciseZip(final Exercise exercise, BgTaskListener<byte[]> listener) {
        final String zipUrl = exercise.getDownloadUrl();
        final CancellableCallable<byte[]> download = networkTasks.downloadBinaryFile(zipUrl);
        return BgTask.start("Downloading " + zipUrl, listener, download);
    }
    
    public Future<SubmissionResult> startSubmittingExercise(final Exercise exercise, final byte[] sourceZip, BgTaskListener<SubmissionResult> listener) {
        final String submitUrl = exercise.getReturnUrl();
        
        Map<String, String> params = Collections.emptyMap();
        final CancellableCallable<String> upload =
                networkTasks.uploadFileForTextResponse(submitUrl, params, "submission[file]", sourceZip);
        
        CancellableCallable<SubmissionResult> task = new CancellableCallable<SubmissionResult>() {
            @Override
            public SubmissionResult call() throws Exception {
                String text = upload.call();
                if (text.isEmpty()) {
                    throw new RuntimeException("Server returned an empty response.");
                }
                return submissionResultParser.parseFromJson(text);
            }

            @Override
            public boolean cancel() {
                return upload.cancel();
            }
        };
        
        return BgTask.start("Submitting " + exercise.getName(), listener, task);
    }
}
