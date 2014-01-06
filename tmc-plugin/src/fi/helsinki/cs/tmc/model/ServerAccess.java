package fi.helsinki.cs.tmc.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.FeedbackAnswer;
import fi.helsinki.cs.tmc.data.Review;
import fi.helsinki.cs.tmc.data.serialization.CourseInfoParser;
import fi.helsinki.cs.tmc.data.serialization.CourseListParser;
import fi.helsinki.cs.tmc.data.serialization.ReviewListParser;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import fi.helsinki.cs.tmc.utilities.ByteArrayGsonSerializer;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.ExceptionUtils;
import fi.helsinki.cs.tmc.utilities.UriUtils;
import fi.helsinki.cs.tmc.utilities.http.FailedHttpResponseException;
import fi.helsinki.cs.tmc.utilities.http.HttpTasks;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.openide.modules.Modules;

/**
 * A frontend for the server.
 */
public class ServerAccess {
    public static final int API_VERSION = 6;
    
    private TmcSettings settings;
    private CourseListParser courseListParser;
    private CourseInfoParser courseInfoParser;
    private ReviewListParser reviewListParser;
    private String clientVersion;
    
    public ServerAccess() {
        this(TmcSettings.getDefault());
    }

    public ServerAccess(TmcSettings settings) {
        this(settings, new CourseListParser(), new CourseInfoParser(), new ReviewListParser());
    }

    public ServerAccess(
        TmcSettings settings,
        CourseListParser courseListParser,
        CourseInfoParser courseInfoParser,
        ReviewListParser reviewListParser
    ) {
        this.settings = settings;
        this.courseListParser = courseListParser;
        this.courseInfoParser = courseInfoParser;
        this.reviewListParser = reviewListParser;
        this.clientVersion = getClientVersion();
    }
    
    private static String getClientVersion() {
        return Modules.getDefault().ownerOf(ServerAccess.class).getSpecificationVersion().toString();
    }
    
    public void setSettings(TmcSettings settings) {
        this.settings = settings;
    }
    
    private String getCourseListUrl() {
        return addApiCallQueryParameters(settings.getServerBaseUrl() + "/courses.json");
    }
    
    private String addApiCallQueryParameters(String url) {
        url = UriUtils.withQueryParam(url, "api_version", ""+API_VERSION);
        url = UriUtils.withQueryParam(url, "client", "netbeans_plugin");
        url = UriUtils.withQueryParam(url, "client_version", clientVersion);
        return url;
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
        final CancellableCallable<String> download = createHttpTasks().getForText(getCourseListUrl());
        return new CancellableCallable<List<Course>>() {
            @Override
            public List<Course> call() throws Exception {
                try {
                    String text = download.call();
                    return courseListParser.parseFromJson(text);
                } catch (FailedHttpResponseException ex) {
                    return checkForObsoleteClient(ex);
                }
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
    }

    public CancellableCallable<Course> getFullCourseInfoTask(Course courseStub) {
        String url = addApiCallQueryParameters(courseStub.getDetailsUrl());
        final CancellableCallable<String> download = createHttpTasks().getForText(url);
        return new CancellableCallable<Course>() {
            @Override
            public Course call() throws Exception {
                try {
                    String text = download.call();
                    return courseInfoParser.parseFromJson(text);
                } catch (FailedHttpResponseException ex) {
                    return checkForObsoleteClient(ex);
                }
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
    }
    
    public CancellableCallable<Void> getUnlockingTask(Course course) {
        Map<String, String> params = Collections.emptyMap();
        final CancellableCallable<String> download = createHttpTasks().postForText(getUnlockUrl(course), params);
        return new CancellableCallable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    download.call();
                    return null;
                } catch (FailedHttpResponseException ex) {
                    return checkForObsoleteClient(ex);
                }
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
    }
    
    private String getUnlockUrl(Course course) {
        return addApiCallQueryParameters(course.getUnlockUrl());
    }
    
    public CancellableCallable<byte[]> getDownloadingExerciseZipTask(Exercise exercise) {
        String zipUrl = exercise.getDownloadUrl();
        return createHttpTasks().getForBinary(zipUrl);
    }
    
    public CancellableCallable<byte[]> getDownloadingExerciseSolutionZipTask(Exercise exercise) {
        String zipUrl = exercise.getSolutionDownloadUrl();
        return createHttpTasks().getForBinary(zipUrl);
    }

    public CancellableCallable<SubmissionResponse> getSubmittingExerciseTask(final Exercise exercise, final byte[] sourceZip, Map<String, String> extraParams) {
        final String submitUrl = addApiCallQueryParameters(exercise.getReturnUrl());
        
        final CancellableCallable<String> upload =
                createHttpTasks().uploadFileForTextDownload(submitUrl, extraParams, "submission[file]", sourceZip);
        
        return new CancellableCallable<SubmissionResponse>() {
            @Override
            public SubmissionResponse call() throws Exception {
                String response;
                try {
                    response = upload.call();
                } catch (FailedHttpResponseException ex) {
                    return checkForObsoleteClient(ex);
                }
                
                JsonObject respJson = new JsonParser().parse(response).getAsJsonObject();
                if (respJson.get("error") != null) {
                    throw new RuntimeException("Server responded with error: " + respJson.get("error"));
                } else if (respJson.get("submission_url") != null) {
                    try {
                        URI submissionUrl = new URI(respJson.get("submission_url").getAsString());
                        URI pasteUrl = new URI(respJson.get("paste_url").getAsString());
                        return new SubmissionResponse(submissionUrl, pasteUrl);
                    } catch (Exception e) {
                        throw new RuntimeException("Server responded with malformed submission url");
                    }
                } else {
                    throw new RuntimeException("Server returned unknown response");
                }
            }

            @Override
            public boolean cancel() {
                return upload.cancel();
            }
        };
    }

    public static class SubmissionResponse {
        public final URI submissionUrl;
        public final URI pasteUrl;
        public SubmissionResponse(URI submissionUrl, URI pasteUrl) {
            this.submissionUrl = submissionUrl;
            this.pasteUrl = pasteUrl;
        }
    }
    
    public CancellableCallable<String> getSubmissionFetchTask(String submissionUrl) {
        return createHttpTasks().getForText(submissionUrl);
    }
    
    public CancellableCallable<List<Review>> getDownloadingReviewListTask(Course course) {
        String url = addApiCallQueryParameters(course.getReviewsUrl());
        final CancellableCallable<String> download = createHttpTasks().getForText(url);
        return new CancellableCallable<List<Review>>() {
            @Override
            public List<Review> call() throws Exception {
                try {
                    String text = download.call();
                    return reviewListParser.parseFromJson(text);
                } catch (FailedHttpResponseException ex) {
                    return checkForObsoleteClient(ex);
                }
            }

            @Override
            public boolean cancel() {
                return download.cancel();
            }
        };
    }
    
    public CancellableCallable<Void> getMarkingReviewAsReadTask(Review review, boolean read) {
        String url = addApiCallQueryParameters(review.getUpdateUrl() + ".json");
        Map<String, String> params = new HashMap<String, String>();
        params.put("_method", "put");
        if (read) {
            params.put("mark_as_read", "1");
        } else {
            params.put("mark_as_unread", "1");
        }
        
        final CancellableCallable<String> task = createHttpTasks().postForText(url, params);
        return new CancellableCallable<Void>() {
            @Override
            public Void call() throws Exception {
                task.call();
                return null;
            }

            @Override
            public boolean cancel() {
                return task.cancel();
            }
        };
    }
    
    public CancellableCallable<String> getFeedbackAnsweringJob(String answerUrl, List<FeedbackAnswer> answers) {
        final String submitUrl = addApiCallQueryParameters(answerUrl);
        
        Map<String, String> params = new HashMap<String, String>();
        for (int i = 0; i < answers.size(); ++i) {
            String keyPrefix = "answers[" + i + "]";
            FeedbackAnswer answer = answers.get(i);
            params.put(keyPrefix + "[question_id]", "" + answer.getQuestion().getId());
            params.put(keyPrefix + "[answer]", answer.getAnswer());
        }
        
        final CancellableCallable<String> upload = createHttpTasks().postForText(submitUrl, params);
        
        return new CancellableCallable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    return upload.call();
                } catch (FailedHttpResponseException ex) {
                    return checkForObsoleteClient(ex);
                }
            }

            @Override
            public boolean cancel() {
                return upload.cancel();
            }
        };
    }
    
    public CancellableCallable<Object> getSendEventLogJob(String spywareServerUrl, List<LoggableEvent> events) {
        String fullUrl = addApiCallQueryParameters(spywareServerUrl);
        fullUrl = UriUtils.withQueryParam(fullUrl, "username", settings.getUsername());
        fullUrl = UriUtils.withQueryParam(fullUrl, "password", settings.getPassword());

        byte[] data;
        try {
            data = eventListToPostBody(events);
        } catch (IOException e) {
            throw ExceptionUtils.toRuntimeException(e);
        }

        final CancellableCallable<String> upload = createHttpTasks().rawPostForText(fullUrl, data);

        return new CancellableCallable<Object>() {
            @Override
            public Object call() throws Exception {
                upload.call();
                return null;
            }

            @Override
            public boolean cancel() {
                return upload.cancel();
            }
        };
    }

    private byte[] eventListToPostBody(List<LoggableEvent> events) throws IOException {
        ByteArrayOutputStream bufferBos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = new GZIPOutputStream(bufferBos);
        OutputStreamWriter bufferWriter = new OutputStreamWriter(gzos, Charset.forName("UTF-8"));

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(byte[].class, new ByteArrayGsonSerializer())
                .create();

        gson.toJson(events, new TypeToken<List<LoggableEvent>>(){}.getType(), bufferWriter);
        bufferWriter.close();
        gzos.close();

        return bufferBos.toByteArray();
    }
    
    private <T> T checkForObsoleteClient(FailedHttpResponseException ex) throws ObsoleteClientException, FailedHttpResponseException {
        if (ex.getStatusCode() == 404) {
            boolean obsolete;
            try {
                obsolete = new JsonParser().parse(ex.getEntityAsString()).getAsJsonObject().get("obsolete_client").getAsBoolean();
            } catch (Exception ex2) {
                obsolete = false;
            }
            if (obsolete) {
                throw new ObsoleteClientException();
            }
        }

        throw ex;
    }
}
