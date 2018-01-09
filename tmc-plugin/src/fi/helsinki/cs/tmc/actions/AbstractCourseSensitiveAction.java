package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.events.TmcEventBus;
import fi.helsinki.cs.tmc.core.events.TmcEventListener;
import fi.helsinki.cs.tmc.model.CourseDb;

import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

public abstract class AbstractCourseSensitiveAction extends NodeAction {
    
    public AbstractCourseSensitiveAction() {
        TmcEventBus.getDefault().subscribeDependent(new TmcEventListener() {
            public void receive(CourseDb.ChangedEvent event) throws Throwable {
                setEnabled(CourseDb.getInstance().getCurrentCourse() != null);
            }
        }, this);
    }
    
    @Override
    protected boolean enable(Node[] nodes) {
        return CourseDb.getInstance().getCurrentCourse() != null;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
}
