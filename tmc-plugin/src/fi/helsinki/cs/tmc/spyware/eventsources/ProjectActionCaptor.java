package fi.helsinki.cs.tmc.spyware.eventsources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ActionProvider;
import org.openide.util.Lookup;
import static org.netbeans.spi.project.ActionProvider.*;

// Hmm, we could eventually override the default test action on a project,
// so there would no longer be a non-TMC and a TMC test action for TMC projects.

/**
 * Acts as a proxy to a project's default ActionProvider and provides
 * a simple interface for listeners.
 *
 * <p>
 * Implementation details:
 * This is registered in layer.xml instead of with <code>@ProjectServiceProvider</code>,
 * because the annotation doesn't allow us to specify a position.
 * We need to be called first, or otherwise we'll be skipped in favor
 * of the actual action provider.
 */
public class ProjectActionCaptor implements ActionProvider {
    public interface Listener {
        void actionInvoked(Project project, String command);
    }
    private static List<Listener> listeners = new ArrayList<Listener>();

    public synchronized static void addListener(Listener listener) {
        listeners.add(listener);
    }

    public synchronized static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private synchronized static void fireEvent(Project p, String cmd) {
        for (Listener listener : listeners) {
            listener.actionInvoked(p, cmd);
        }
    }

    private final Project project;
    private boolean beingCalled; // Prevent recursion when proxying

    public ProjectActionCaptor(Project project) {
        this.project = project;
        this.beingCalled = false;
    }

    @Override
    public String[] getSupportedActions() {
        return new String[] {
            COMMAND_CLEAN,
            COMMAND_BUILD,
            COMMAND_REBUILD,
            COMMAND_RUN,
            COMMAND_RUN_SINGLE,
            COMMAND_DEBUG,
            COMMAND_DEBUG_SINGLE,
            COMMAND_PROFILE,
            COMMAND_PROFILE_SINGLE,
            COMMAND_TEST,
            COMMAND_TEST_SINGLE,
        };
    }

    @Override
    public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {
        if (beingCalled) {
            return false;
        }

        try {
            beingCalled = true;
            for (ActionProvider provider : project.getLookup().lookupAll(ActionProvider.class)) {
                if (Arrays.asList(provider.getSupportedActions()).contains(command) &&
                        provider.isActionEnabled(command, context)) {
                    return true;
                }
            }
            return false;
        } finally {
            beingCalled = false;
        }
    }

    @Override
    public void invokeAction(String command, Lookup context) throws IllegalArgumentException {
        if (!beingCalled) {
            beingCalled = true;
            if (command.equals(COMMAND_RUN)){
                int a = 1+1;
            }
            try {
                for (ActionProvider provider : project.getLookup().lookupAll(ActionProvider.class)) {
                    if (Arrays.asList(provider.getSupportedActions()).contains(command) &&
                            provider.isActionEnabled(command, context)) {
                        provider.invokeAction(command, context);
                    }
                }
                fireEvent(project, command);
            } finally {
                beingCalled = false;
            }
        }
    }
}
