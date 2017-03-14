package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.coreimpl.BridgingProgressObserver;
import fi.helsinki.cs.tmc.utilities.BgTask;
import java.util.concurrent.Callable;

public class SendDiagnostics {
    
    public void run() {
        ProgressObserver observer = new BridgingProgressObserver();
        Callable<Void> sendDiagnostics = TmcCore.get().sendDiagnostics(observer);
        BgTask.start("Sending diagnostics", sendDiagnostics);
    }
}
