package fi.helsinki.cs.tmc.utilities;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils {
    public static String backtraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
