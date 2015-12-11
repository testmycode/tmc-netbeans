package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.data.FeedbackAnswer;
import fi.helsinki.cs.tmc.core.domain.Review;
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
import fi.helsinki.cs.tmc.core.TmcCore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.openide.modules.Modules;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * A frontend for the server.
 *
 * @deprecated Users are suggested to rely on the tmc-core instead of this implementation.
 */
@Deprecated
public class ServerAccess {
    public static final int API_VERSION = 7;

    private NbTmcSettings settings;
    private CourseListParser courseListParser;
    private CourseInfoParser courseInfoParser;
    private ReviewListParser reviewListParser;
    private String clientVersion;
    private TmcCore tmcCore;

    public ServerAccess() {
        this(NbTmcSettings.getDefault());
    }

    public ServerAccess(NbTmcSettings settings) {
        this(settings, new CourseListParser(), new CourseInfoParser(), new ReviewListParser());
    }

    public ServerAccess(
            NbTmcSettings settings,
            CourseListParser courseListParser,
            CourseInfoParser courseInfoParser,
            ReviewListParser reviewListParser) {
        this.settings = settings;
        this.courseListParser = courseListParser;
        this.courseInfoParser = courseInfoParser;
        this.reviewListParser = reviewListParser;
        this.clientVersion = getClientVersion();
    }

    private static String getClientVersion() {
        return Modules.getDefault()
                .ownerOf(ServerAccess.class)
                .getSpecificationVersion()
                .toString();
    }

    public void setSettings(NbTmcSettings settings) {
        this.settings = settings;
    }

    private String getCourseListUrl() {
        return addApiCallQueryParameters(settings.getServerAddress() + "/courses.json");
    }

    public String addApiCallQueryParameters(String url) {
        url = UriUtils.withQueryParam(url, "api_version", "" + API_VERSION);
        url = UriUtils.withQueryParam(url, "client", "netbeans_plugin");
        url = UriUtils.withQueryParam(url, "client_version", getClientVersion());
        return url;
    }

    private HttpTasks createHttpTasks() {
        return new HttpTasks().setCredentials(settings.getUsername(), settings.getPassword());
    }

    public boolean hasEnoughSettings() {
        return !settings.getUsername().isEmpty()
                && !settings.getPassword().isEmpty()
                && !settings.getServerAddress().isEmpty();
    }

    public boolean needsOnlyPassword() {
        return !settings.getUsername().isEmpty()
                && settings.getPassword().isEmpty()
                && !settings.getServerAddress().isEmpty();
    }

    @Deprecated
    public CancellableCallable<List<Course>> getDownloadingCourseListTask() {
        final CancellableCallable<String> download =
                createHttpTasks().getForText(getCourseListUrl());
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

    @Deprecated
    public CancellableCallable<Course> getFullCourseInfoTask(Course courseStub) {
        URI url = URI.create(addApiCallQueryParameters(courseStub.getDetailsUrl().toString()));
        final CancellableCallable<String> download = createHttpTasks().getForText(url.toString());
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
        final CancellableCallable<String> download =
                createHttpTasks().postForText(getUnlockUrl(course), params);
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
        return addApiCallQueryParameters(course.getUnlockUrl().toString());
    }

    public CancellableCallable<byte[]> getDownloadingExerciseZipTask(Exercise exercise) {
        URI zipUrl = exercise.getDownloadUrl();
        return createHttpTasks().getForBinary(zipUrl.toString());
    }

    public CancellableCallable<byte[]> getDownloadingExerciseSolutionZipTask(Exercise exercise) {
        URI zipUrl = exercise.getSolutionDownloadUrl();
        return createHttpTasks().getForBinary(zipUrl.toString());
    }

    public CancellableCallable<SubmissionResponse> getSubmittingExerciseTask(
            final Exercise exercise, final byte[] sourceZip, Map<String, String> extraParams) {
        final URI submitUrl = URI.create(addApiCallQueryParameters(exercise.getReturnUrl().toString()));

        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("client_time", "" + (System.currentTimeMillis() / 1000L));
        params.put("client_nanotime", "" + System.nanoTime());
        params.putAll(extraParams);

        final CancellableCallable<String> upload =
                createHttpTasks()
                        .uploadFileForTextDownload(
                                submitUrl.toString(), params, "submission[file]", sourceZip);

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
                    throw new RuntimeException(
                            "Server responded with error: " + respJson.get("error"));
                } else if (respJson.get("submission_url") != null) {
                    try {
                        URI submissionUrl = new URI(respJson.get("submission_url").getAsString());
                        URI pasteUrl = new URI(respJson.get("paste_url").getAsString());
                        return new SubmissionResponse(submissionUrl, pasteUrl);
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Server responded with malformed submission url");
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
        URI url = URI.create(addApiCallQueryParameters(course.getReviewsUrl().toString()));
        final CancellableCallable<String> download = createHttpTasks().getForText(url.toString());
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

    public CancellableCallable<String> getFeedbackAnsweringJob(
            String answerUrl, List<FeedbackAnswer> answers) {
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

    public CancellableCallable<Object> getSendEventLogJob(
            String spywareServerUrl, List<LoggableEvent> events) {
        String url = addApiCallQueryParameters(spywareServerUrl);

        Map<String, String> extraHeaders = new LinkedHashMap<String, String>();
        extraHeaders.put("X-Tmc-Version", "1");
        extraHeaders.put("X-Tmc-Username", settings.getUsername());
        extraHeaders.put("X-Tmc-Password", settings.getPassword());

        byte[] data;
        try {
            data = eventListToPostBody(events);
        } catch (IOException e) {
            throw ExceptionUtils.toRuntimeException(e);
        }

        final CancellableCallable<String> upload =
                createHttpTasks().rawPostForText(url, data, extraHeaders);

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

    public byte[] eventListToPostBody(List<LoggableEvent> events) throws IOException {
        ByteArrayOutputStream bufferBos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = new GZIPOutputStream(bufferBos);
        OutputStreamWriter bufferWriter = new OutputStreamWriter(gzos, Charset.forName("UTF-8"));

        Gson gson =
                new GsonBuilder()
                        .registerTypeAdapter(byte[].class, new ByteArrayGsonSerializer()).create();

        gson.toJson(events, new TypeToken<List<LoggableEvent>>() {}.getType(), bufferWriter);
        bufferWriter.close();
        gzos.close();

        return bufferBos.toByteArray();
    }

    private <T> T checkForObsoleteClient(FailedHttpResponseException ex)
            throws ObsoleteClientException, FailedHttpResponseException {
        if (ex.getStatusCode() == 404) {
            boolean obsolete;
            try {
                obsolete =
                        new JsonParser()
                                .parse(ex.getEntityAsString())
                                .getAsJsonObject()
                                .get("obsolete_client")
                                .getAsBoolean();
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
