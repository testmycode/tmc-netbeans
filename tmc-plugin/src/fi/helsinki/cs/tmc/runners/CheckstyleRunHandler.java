package fi.helsinki.cs.tmc.runners;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.stylerunner.CheckstyleResult;
import fi.helsinki.cs.tmc.stylerunner.CheckstyleRunner;
import fi.helsinki.cs.tmc.ui.CheckstyleResultDisplayer;

import org.netbeans.api.project.Project;

import org.openide.util.Exceptions;

public final class CheckstyleRunHandler {

    private final CheckstyleResultDisplayer checkstyleResultDisplayer = CheckstyleResultDisplayer.getInstance();

    public void performAction(final Project project) {

        final TmcProjectInfo projectInfo = ProjectMediator.getInstance().wrapProject(project);

        try {
            final CheckstyleResult result = new CheckstyleRunner(projectInfo.getProjectDirAsFile()).run();
            checkstyleResultDisplayer.showCheckstyleResult(result);
        } catch (CheckstyleException exception) {
            Exceptions.printStackTrace(exception);
        }
    }
}
