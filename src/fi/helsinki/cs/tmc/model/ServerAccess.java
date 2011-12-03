package fi.helsinki.cs.tmc.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.utilities.http.HttpTasks;
import fi.helsinki.cs.tmc.data.serialization.CourseListParser;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.UriUtils;
import java.net.URI;
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
                new CourseListParser()
                );
    }
    
    private TmcSettings settings;
    private CourseListParser courseListParser;

    public ServerAccess(
            TmcSettings settings,
            CourseListParser courseListParser) {
        this.settings = settings;
        this.courseListParser = courseListParser;
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
    
    public Future<URI> startSubmittingExercise(final Exercise exercise, final byte[] sourceZip, BgTaskListener<URI> listener) {
        final String submitUrl = addApiCallQueryParameters(exercise.getReturnUrl());
        
        Map<String, String> params = Collections.emptyMap();
        final CancellableCallable<String> upload =
                createHttpTasks().uploadFileForTextDownload(submitUrl, params, "submission[file]", sourceZip);
        
        final CancellableCallable<URI> task = new CancellableCallable<URI>() {
            @Override
            public URI call() throws Exception {
                String response = upload.call();
                JsonObject respJson = new JsonParser().parse(response).getAsJsonObject();
                if (respJson.get("error") != null) {
                    throw new RuntimeException("Server responded with error: " + respJson.get("error"));
                } else if (respJson.get("submission_url") != null) {
                    try {
                        return new URI(respJson.get("submission_url").getAsString());
                    } catch (Exception e) {
                        throw new RuntimeException("Server responded with malformed submission url");
                    }
                } else {
                    throw new RuntimeException("Server returned unknown response");
                }
            }

            @Override
            public boolean cancel() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
        
        return BgTask.start("Submitting " + exercise.getName(), listener, task);
    }
    
    public CancellableCallable<String> getSubmissionFetchJob(URI submissionUrl) {
        return createHttpTasks().downloadTextFile(submissionUrl.toString());
    }
}
