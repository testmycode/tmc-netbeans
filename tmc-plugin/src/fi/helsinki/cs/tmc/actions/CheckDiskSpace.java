package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.utilities.SingletonTask;
import fi.helsinki.cs.tmc.core.utilities.TmcRequestProcessor;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TmcNotificationDisplayer;
import fi.helsinki.cs.tmc.ui.TmcNotificationDisplayer.SingletonToken;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.function.Consumer;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.ImageUtilities;

public class CheckDiskSpace {

    private static final long DEFAULT_CHECK_INTERVAL = 10 * 60 * 1000;

    private final TmcCoreSettingsImpl settings;
    private final SingletonToken notifierToken;

    public CheckDiskSpace(TmcCoreSettingsImpl settings) {
        this.settings = settings;
        this.notifierToken = TmcNotificationDisplayer.createSingletonToken();
    }

    public void startCheckingPeriodically() {
        SingletonTask periodicChecker = new SingletonTask(() -> {
            doChecks((message) -> {
                TmcNotificationDisplayer.getDefault().notify(notifierToken, "You're running out of disk space", ImageUtilities.loadImageIcon("fi/helsinki/cs/tmc/ui/infobubble.png", false), message, null, NotificationDisplayer.Priority.HIGH);
            });
        }, TmcRequestProcessor.instance);
        periodicChecker.setInterval(DEFAULT_CHECK_INTERVAL);
    }

    public void run() {
        doChecks((message) -> {
            ConvenientDialogDisplayer.getDefault().displayWarning(message);
        });
    }

    private void doChecks(Consumer<String> onMessage) {
        try {
            final FileStore fileStore = Files.getFileStore(settings.getConfigRoot());
            // Gigabytes
            final double usableSpace = fileStore.getUsableSpace() / 1000000000.0;
            if (usableSpace < 1) {
                showDiskWarning(fileStore, usableSpace, onMessage);
            } else {
                final FileStore projectFileStore = Files.getFileStore(Paths.get(settings.getProjectRootDir()));
                final double projectUsableSpace = projectFileStore.getUsableSpace() / 1000000000.0;
                if (projectUsableSpace < 1) {
                    showDiskWarning(projectFileStore, projectUsableSpace, onMessage);
                }
            }
        } catch (IOException ex) {
        }
    }

    private void showDiskWarning(FileStore store, double usableSpace, Consumer<String> notifier) {
        final String formattedSpaceAvailable = new DecimalFormat("##.##").format(usableSpace);
        final String fileStoreName = store.name();
        final String message = "Your computer has only " + formattedSpaceAvailable + "GB of free disk space available.\n"
                + "Consider freeing up some space by deleting unused files or this program may not work properly!\n"
                + "\nThe lack of free space was detected in " + fileStoreName + ".";
        notifier.accept(message);
    }
}
