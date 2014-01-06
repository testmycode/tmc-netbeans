package fi.helsinki.cs.tmc.spyware;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fi.helsinki.cs.tmc.model.ConfigFile;
import fi.helsinki.cs.tmc.utilities.ByteArrayGsonSerializer;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventStore {
    private static final Logger log = Logger.getLogger(EventStore.class.getName());
    
    private ConfigFile configFile;

    public EventStore() {
        this.configFile = new ConfigFile("Events.json");
    }
    
    public void save(List<LoggableEvent> events) throws IOException {
        String text = getGson().toJson(events);
        configFile.writeContents(text);
        log.log(Level.INFO, "Saved {0} events", events.size());
    }
    
    public List<LoggableEvent> load() throws IOException {
        String text = configFile.readContents();
        List<LoggableEvent> result = getGson().fromJson(text, new TypeToken<List<LoggableEvent>>() {}.getType());
        if (result == null) {
            result = Collections.emptyList();
        }
        log.log(Level.INFO, "Loaded {0} events", result.size());
        return result;
    }
    
    private Gson getGson() {
        return new GsonBuilder()
            .registerTypeAdapter(byte[].class, new ByteArrayGsonSerializer())
            .create();
    }
    
    public void clear() throws IOException {
        configFile.writeContents("");
    }
}
