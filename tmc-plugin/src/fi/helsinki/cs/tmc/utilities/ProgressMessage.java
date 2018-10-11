package fi.helsinki.cs.tmc.utilities;

import com.google.common.base.Optional;

public class ProgressMessage {

    private final String message;

    private final Optional<Double> percentDone;

    public ProgressMessage(String message, Optional<Double> percentDone) {
        this.message = message;
        this.percentDone = percentDone;
    }

    public String getMessage() {
        return message;
    }

    public Optional<Double> getPercentDone() {
        return percentDone;
    }
}
