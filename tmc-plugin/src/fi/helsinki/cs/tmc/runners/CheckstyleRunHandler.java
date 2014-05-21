package fi.helsinki.cs.tmc.runners;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.stylerunner.CheckstyleRunner;
import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.TestResultDisplayer;
import fi.helsinki.cs.tmc.ui.ValidationResultDisplayer;

import org.netbeans.api.project.Project;

import org.openide.util.Exceptions;

public final class CheckstyleRunHandler {

    private final ValidationResultDisplayer validationResultDisplayer = ValidationResultDisplayer.getInstance();

    public void performAction(final Project project) {

        final TmcProjectInfo projectInfo = ProjectMediator.getInstance().wrapProject(project);
        final String projectType = projectInfo.getProjectType().name();

        if (!projectType.equals("JAVA_SIMPLE") && !projectType.equals("JAVA_MAVEN")) {
            return;
        }

        // Save all files
        ProjectMediator.getInstance().saveAllFiles();

        try {

            final ValidationResult result = new CheckstyleRunner(projectInfo.getProjectDirAsFile()).run();

            // Cannot submit when validation errors exist
            TestResultDisplayer.getInstance().canSubmit(result.getValidationErrors().isEmpty());

            validationResultDisplayer.showValidationResult(result);

        } catch (CheckstyleException exception) {
            ConvenientDialogDisplayer.getDefault().displayError("Checkstyle audit failed.");
            Exceptions.printStackTrace(exception);
        }
    }
}
