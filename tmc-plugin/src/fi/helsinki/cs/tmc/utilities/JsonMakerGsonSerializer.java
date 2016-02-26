package fi.helsinki.cs.tmc.utilities;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import org.apache.commons.codec.binary.Base64;

/**
 * Converts JsonMaker to byte[] to base64 in JSON.
 */

public class JsonMakerGsonSerializer implements JsonSerializer<JsonMaker> /*, JsonDeserializer<JsonMaker> */ {
    @Override
    public JsonElement serialize(JsonMaker data, Type type, JsonSerializationContext jsc) {

        if (data == null) {
            return JsonNull.INSTANCE;
        } else {
            return new JsonPrimitive(Base64.encodeBase64String(data.toString().getBytes(Charset.forName("UTF-8"))));
        }
    }

//    @Override
//    public JsonMaker deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
//        return null;
//    }
}
