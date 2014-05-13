package fi.helsinki.cs.tmc.runners;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.stylerunner.CheckstyleResult;
import fi.helsinki.cs.tmc.stylerunner.CheckstyleRunner;
import fi.helsinki.cs.tmc.ui.CheckstyleResultDisplayer;
import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;

public class CheckstyleRunHandler {

    private CheckstyleResultDisplayer checkstyleResultDisplayer = CheckstyleResultDisplayer.getInstance();

    public void performAction(Project project) {

        final TmcProjectInfo projectInfo = ProjectMediator.getInstance().wrapProject(project);

        try {
            CheckstyleResult result = new CheckstyleRunner(projectInfo.getProjectDirAsFile()).run();
            checkstyleResultDisplayer.showCheckstyleResults(result);
        } catch (CheckstyleException exception) {
            Exceptions.printStackTrace(exception);
        }
    }
}
