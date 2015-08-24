package fi.helsinki.cs.tmc.spyware;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.helsinki.cs.tmc.model.ConfigFile;
import fi.helsinki.cs.tmc.utilities.ByteArrayGsonSerializer;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventStore {
    private static final Logger log = Logger.getLogger(EventStore.class.getName());

    private ConfigFile configFile;

    public EventStore() {
        this.configFile = new ConfigFile("Events.json");
    }

    public void save(LoggableEvent[] events) throws IOException {
        String text = getGson().toJson(events);
        configFile.writeContents(text);
        log.log(Level.INFO, "Saved {0} events", events.length);
    }

    public LoggableEvent[] load() throws IOException {
        String text = configFile.readContents();
        LoggableEvent[] result = getGson().fromJson(text, LoggableEvent[].class);
        if (result == null) {
            result = new LoggableEvent[0];
        }
        log.log(Level.INFO, "Loaded {0} events", result.length);
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