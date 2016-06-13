package fi.helsinki.cs.tmc.data.serialization;

import fi.helsinki.cs.tmc.core.domain.Review;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ReviewListParser {
    private static class ReviewListContainer {
        public int apiVersion;
        public Review[] reviews;
    }
    
    public List<Review> parseFromJson(String json) {
        if (json == null) {
            throw new NullPointerException("Json string is null");
        }
        if (json.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty input");
        }
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Date.class, new CustomDateDeserializer())
                    .create();
            
            Review[] reviews = gson.fromJson(json, ReviewListContainer.class).reviews;
            return Arrays.asList(reviews);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to parse review list: " + e.getMessage(), e);
        }
    }
}
