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
import fi.helsinki.cs.tmc.utilities.UriUtils;
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
                TmcSettings.getDefault(),
                new CourseListParser(),
                new SubmissionResultParser()
                );
    }
    
    private TmcSettings settings;
    private CourseListParser courseListParser;
    private SubmissionResultParser submissionResultParser;

    public ServerAccess(
            TmcSettings settings,
            CourseListParser courseListParser,
            SubmissionResultParser submissionResultParser) {
        this.settings = settings;
        this.courseListParser = courseListParser;
        this.submissionResultParser = submissionResultParser;
    }
    
    public void setSettings(TmcSettings settings) {
        this.settings = settings;
    }
    
    private String getCourseListUrl() {
        return addApiCallQueryParameters(settings.getServerBaseUrl() + "/courses.json");
    }
    
    private String addApiCallQueryParameters(String url) {
        return UriUtils.withQueryParam(url, "api_version", ""+API_VERSION);
    }
    
    private HttpTasks createHttpTasks() {
        return new HttpTasks().setCredentials(settings.getUsername(), settings.getPassword());
    }
    
    public boolean hasEnoughSettings() {
        return
                !settings.getUsername().isEmpty() &&
                !settings.getPassword().isEmpty() &&
                !settings.getServerBaseUrl().isEmpty();
    }
    
    public Future<CourseList> startDownloadingCourseList(BgTaskListener<CourseList> listener) {
        final CancellableCallable<String> download = createHttpTasks().downloadTextFile(getCourseListUrl());
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
        final CancellableCallable<byte[]> download = createHttpTasks().downloadBinaryFile(zipUrl);
        return BgTask.start("Downloading " + zipUrl, listener, download);
    }
    
    public Future<SubmissionResult> startSubmittingExercise(final Exercise exercise, final byte[] sourceZip, BgTaskListener<SubmissionResult> listener) {
        final String submitUrl = addApiCallQueryParameters(exercise.getReturnUrl());
        
        Map<String, String> params = Collections.emptyMap();
        final CancellableCallable<String> upload =
                createHttpTasks().uploadFileForTextResponse(submitUrl, params, "submission[file]", sourceZip);
        
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
