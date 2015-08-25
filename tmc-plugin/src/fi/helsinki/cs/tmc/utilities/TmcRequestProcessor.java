package fi.helsinki.cs.tmc.utilities;

import org.openide.util.RequestProcessor;

/** Holds TMC's default RequestProcessor. */
public class TmcRequestProcessor {
    public static final RequestProcessor instance =
        new RequestProcessor("TMC request processor", 5, true);
}