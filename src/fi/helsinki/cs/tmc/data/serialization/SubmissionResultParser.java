package fi.helsinki.cs.tmc.data.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import fi.helsinki.cs.tmc.data.SubmissionResult;
import fi.helsinki.cs.tmc.testrunner.StackTraceSerializer;
import java.lang.reflect.Type;

public class SubmissionResultParser {
    
    public SubmissionResult parseFromJson(String json) {
        if (json.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty input");
        }
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(SubmissionResult.Status.class, new StatusDeserializer())
                    .registerTypeAdapter(StackTraceElement.class, new StackTraceSerializer())
                    .create();

            return gson.fromJson(json, SubmissionResult.class);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to parse submission result: " + e.getMessage(), e);
        }
    }
    
    private static class StatusDeserializer implements JsonDeserializer<SubmissionResult.Status> {
        @Override
        public SubmissionResult.Status deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String s = json.getAsJsonPrimitive().getAsString();
            try {
                return SubmissionResult.Status.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Unknown submission status: " + s);
            }
        }
    }
}
