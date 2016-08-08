package fi.helsinki.cs.tmc.coreimpl;

import fi.helsinki.cs.tmc.core.domain.ProgressObserver;

import org.netbeans.api.progress.ProgressHandle;

public class BridgingProgressObserver extends ProgressObserver {

    ProgressHandle handle;

    @Override
    public void progress(long id, String progressMessage) {
        System.out.println("progtess: " + progressMessage);
        if (handle != null) {
            System.out.println("notnull" );
            handle.progress(progressMessage);
        }
    }

    @Override
    public void progress(long id, Double percentDone, String progressMessage) {
        System.out.println("progtess: " + progressMessage);
        if (handle != null) {
            System.out.println("notnull" );
            handle.progress(progressMessage);
        }
    }

    @Override
    public void start(long id) {
        // NOP
        System.out.println(id + " started");
    }

    @Override
    public void end(long id) {
        // NOP
        System.out.println(id + " ended");
    }

    public void attach(ProgressHandle progressHandle) {
        this.handle = progressHandle;
    }
}
