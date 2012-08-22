package fi.helsinki.cs.tmc.testing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonBuilder {

    public static interface Jsonable {
        public JsonElement toJson();
    }
    
    public static class Prop {
        public final String key;
        public final JsonElement value;
        
        public Prop(String key, JsonElement value) {
            this.key = key;
            this.value = value;
        }
    }
    
    public static Prop prop(String key, Object value) {
        return new Prop(key, toJson(value));
    }
    
    public static JsonObject object(Prop... props) {
        JsonObject obj = new JsonObject();
        for (Prop prop : props) {
            obj.add(prop.key, prop.value);
        }
        return obj;
    }
    
    public static JsonArray array(Object... elements) {
        JsonArray result = new JsonArray();
        for (int i = 0; i < elements.length; ++i) {
            result.add(toJson(elements[i]));
        }
        return result;
    }
    
    private static JsonElement toJson(Object value) {
        if (value instanceof JsonElement) {
            return (JsonElement)value;
        } else if (value instanceof Jsonable) {
            return ((Jsonable)value).toJson();
        } else {
            return gson().toJsonTree(value);
        }
    }
    
    private static Gson gson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }
}
