package fi.helsinki.cs.tmc.resources;

import fi.helsinki.cs.tmc.model.TmcSettings;

import java.util.ResourceBundle;

public final class LocalizedMessage {

    private static final String MESSAGES = "fi.helsinki.cs.tmc.resources.messages";

    private LocalizedMessage() {}

    public static String getMessage(final String key) {

        ResourceBundle bundle = ResourceBundle.getBundle(MESSAGES, TmcSettings.getDefault().getErrorMsgLocale());

        return bundle.getString(key);
    }

    public static String getFormattedMessage(final String key, Object... arguments) {

        return String.format(getMessage(key), arguments);
    }
}
