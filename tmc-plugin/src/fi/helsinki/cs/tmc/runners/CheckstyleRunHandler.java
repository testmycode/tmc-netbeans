package fi.helsinki.cs.tmc.runners;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.stylerunner.CheckstyleRunner;
import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;

public class CheckstyleRunHandler {

    public void performAction(Project project) {

        final TmcProjectInfo projectInfo = ProjectMediator.getInstance().wrapProject(project);

        try {
            new CheckstyleRunner(projectInfo.getProjectDirAsFile()).run();
        } catch (CheckstyleException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
