package fi.helsinki.cs.tmc.runners;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.data.serialization.TestResultParser;
import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.NbTmcSettings;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.execution.ExecutorTask;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public abstract class AbstractExerciseRunner implements ExerciseRunner {

    protected static final String ERROR_MSG_LOCALE_SETTING = "fi.helsinki.cs.tmc.edutestutils.defaultLocale";
    private static final Logger log = Logger.getLogger(AbstractExerciseRunner.class.getName());

    protected NbTmcSettings settings;
    protected CourseDb courseDb;
    protected ProjectMediator projectMediator;
    protected TestResultParser resultParser;
    protected TestResultDisplayer resultDisplayer;
    protected TmcEventBus eventBus;

    public AbstractExerciseRunner() {
        this.settings = NbTmcSettings.getDefault();
        this.courseDb = CourseDb.getInstance();
        this.projectMediator = ProjectMediator.getInstance();
        this.resultParser = new TestResultParser();
        this.resultDisplayer = TestResultDisplayer.getInstance();
        this.eventBus = TmcEventBus.getDefault();
    }

    protected Exercise tryGetExercise(Project project) {
        return projectMediator.tryGetExerciseForProject(projectMediator.wrapProject(project), courseDb);
    }

    protected InputOutput getIoTab() {
        InputOutput inOut = IOProvider.getDefault().getIO("Test output", false);
        try {
            inOut.getOut().reset();
        } catch (IOException e) {
            // Ignore
        }
        if (inOut.isClosed()) {
            inOut.select();
        }
        return inOut;
    }

    protected Callable<Integer> executorTaskToCallable(final ExecutorTask et) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return et.result();
            }
        };
    }
}