package fi.helsinki.cs.tmc.utilities;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.apache.commons.codec.binary.Base64;

import java.lang.reflect.Type;

/**
 * Converts byte[] to/from base64 in JSON.
 */
public class ByteArrayGsonSerializer implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
    @Override
    public JsonElement serialize(byte[] data, Type type, JsonSerializationContext jsc) {
        if (data == null) {
            return JsonNull.INSTANCE;
        } else {
            return new JsonPrimitive(Base64.encodeBase64String(data));
        }
    }

    @Override
    public byte[] deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
        if (je.isJsonPrimitive() && ((JsonPrimitive)je).isString()) {
            return Base64.decodeBase64(je.getAsString());
        } else if (je.isJsonNull()) {
            return null;
        } else {
            throw new JsonParseException("Not a base64 string.");
        }
    }
}