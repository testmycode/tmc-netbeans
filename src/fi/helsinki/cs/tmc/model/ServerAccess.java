package fi.helsinki.cs.tmc.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.utilities.http.HttpTasks;
import fi.helsinki.cs.tmc.data.serialization.CourseListParser;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.UriUtils;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A frontend for the server.
 */
public class ServerAccess {
    private static final int API_VERSION = 1;
    
    private TmcSettings settings;
    private CourseListParser courseListParser;

    public ServerAccess() {
        this(TmcSettings.getDefault());
    }

    public ServerAccess(TmcSettings settings) {
        this(settings, new CourseListParser());
    }

    public ServerAccess(TmcSettings settings, CourseListParser courseListParser) {
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

    public boolean needsOnlyPassword() {
        return
                !settings.getUsername().isEmpty() &&
                settings.getPassword().isEmpty() &&
                !settings.getServerBaseUrl().isEmpty();
    }
    
    public CancellableCallable<List<Course>> getDownloadingCourseListTask() {
        final CancellableCallable<String> download = createHttpTasks().downloadTextFile(getCourseListUrl());
        return new CancellableCallable<List<Course>>() {
            @Override
            public List<Course> call() throws Exception {
                String text = download.call();
                return courseListParser.parseFromJson(text);
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
    }
    
    public CancellableCallable<byte[]> getDownloadingExerciseZipTask(Exercise exercise) {
        String zipUrl = exercise.getDownloadUrl();
        return createHttpTasks().downloadBinaryFile(zipUrl);
    }
    
    public CancellableCallable<URI> getSubmittingExerciseTask(final Exercise exercise, final byte[] sourceZip) {
        final String submitUrl = addApiCallQueryParameters(exercise.getReturnUrl());
        
        Map<String, String> params = Collections.emptyMap();
        final CancellableCallable<String> upload =
                createHttpTasks().uploadFileForTextDownload(submitUrl, params, "submission[file]", sourceZip);
        
        return new CancellableCallable<URI>() {
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
    }
    
    public CancellableCallable<String> getSubmissionFetchJob(URI submissionUrl) {
        return createHttpTasks().downloadTextFile(submissionUrl.toString());
    }
}
