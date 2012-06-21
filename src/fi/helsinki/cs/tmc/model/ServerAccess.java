package fi.helsinki.cs.tmc.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.FeedbackAnswer;
import fi.helsinki.cs.tmc.utilities.http.HttpTasks;
import fi.helsinki.cs.tmc.data.serialization.CourseListParser;
import fi.helsinki.cs.tmc.spyware.LoggableEvent;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.UriUtils;
import fi.helsinki.cs.tmc.utilities.http.FailedHttpResponseException;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openide.modules.Modules;

/**
 * A frontend for the server.
 */
public class ServerAccess {
    public static final int API_VERSION = 4;
    
    private TmcSettings settings;
    private CourseListParser courseListParser;
    private String clientVersion;

    public ServerAccess() {
        this(TmcSettings.getDefault());
    }

    public ServerAccess(TmcSettings settings) {
        this(settings, new CourseListParser());
    }

    public ServerAccess(TmcSettings settings, CourseListParser courseListParser) {
        this.settings = settings;
        this.courseListParser = courseListParser;
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
    
    public CancellableCallable<byte[]> getDownloadingExerciseZipTask(Exercise exercise) {
        String zipUrl = exercise.getDownloadUrl();
        return createHttpTasks().getForBinary(zipUrl);
    }
    
    public CancellableCallable<byte[]> getDownloadingExerciseSolutionZipTask(Exercise exercise) {
        String zipUrl = exercise.getSolutionDownloadUrl();
        return createHttpTasks().getForBinary(zipUrl);
    }
    
    public CancellableCallable<URI> getSubmittingExerciseTask(final Exercise exercise, final byte[] sourceZip) {
        final String submitUrl = addApiCallQueryParameters(exercise.getReturnUrl());
        
        Map<String, String> params = Collections.emptyMap();
        final CancellableCallable<String> upload =
                createHttpTasks().uploadFileForTextDownload(submitUrl, params, "submission[file]", sourceZip);
        
        return new CancellableCallable<URI>() {
            @Override
            public URI call() throws Exception {
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
                return upload.cancel();
            }
        };
    }
    
    public CancellableCallable<String> getSubmissionFetchJob(String submissionUrl) {
        return createHttpTasks().getForText(submissionUrl);
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
    
    public CancellableCallable<Object> getSendEventLogJob(List<LoggableEvent> events) {
        Map<String, String> params = eventsToParams(events);
        byte[] data = concatData(events);
        final CancellableCallable<String> upload = createHttpTasks().uploadFileForTextDownload(getSendEventLogUrl(), params, "data", data);
        
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
    
    private String getSendEventLogUrl() {
        return addApiCallQueryParameters(settings.getServerBaseUrl() + "/student_events.json");
    }
    
    private Map<String, String> eventsToParams(List<LoggableEvent> events) {
        Map<String, String> result = new HashMap<String, String>();
        int dataOffset = 0;
        for (int i = 0; i < events.size(); ++i) {
            LoggableEvent ev = events.get(i);
            String prefix = "events[" + i + "]";
            result.put(prefix + "[course_name]", ev.getCourseName());
            result.put(prefix + "[exercise_name]", ev.getExerciseName());
            result.put(prefix + "[event_type]", ev.getEventType());
            result.put(prefix + "[happened_at]", fmtDate(ev.getHappenedAt()));
            
            result.put(prefix + "[data_offset]", ""+dataOffset);
            result.put(prefix + "[data_length]", ""+ev.getData().length);
            dataOffset += ev.getData().length;
        }
        return result;
    }
    
    private byte[] concatData(List<LoggableEvent> events) {
        int size = 0;
        for (LoggableEvent ev : events) {
            size += ev.getData().length;
        }
        
        byte[] result = new byte[size];
        int i = 0;
        for (LoggableEvent ev : events) {
            System.arraycopy(ev.getData(), 0, result, i, ev.getData().length);
            i += ev.getData().length;
        }
        
        return result;
    }
    
    private String fmtDate(Date date) {
        return new java.sql.Timestamp(date.getTime()).toString();
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
