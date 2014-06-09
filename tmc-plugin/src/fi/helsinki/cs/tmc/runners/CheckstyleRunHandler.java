package fi.helsinki.cs.tmc.runners;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.stylerunner.CheckstyleRunner;
import fi.helsinki.cs.tmc.stylerunner.validation.CheckstyleResult;
import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.ui.ValidationResultDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;

import java.util.Locale;

import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;

public final class CheckstyleRunHandler implements Runnable {

    private Project project;
    private final ConvenientDialogDisplayer dialogDisplayer = ConvenientDialogDisplayer.getDefault();
    private final ValidationResultDisplayer validationResultDisplayer = ValidationResultDisplayer.getInstance();

    public void performAction(final Project project) {

        this.project = project;

        BgTask.start("Running validations", this, new BgTaskListener<Object>() {

            @Override
            public void bgTaskFailed(final Throwable exception) {

                dialogDisplayer.displayError("Failed to validate the code.");
            }

            @Override
            public void bgTaskCancelled() {}

            @Override
            public void bgTaskReady(final Object result) {}
        });
    }

    @Override
    public void run() {

        final TmcProjectInfo projectInfo = ProjectMediator.getInstance().wrapProject(project);
        final String projectType = projectInfo.getProjectType().name();

        if (!projectType.equals("JAVA_SIMPLE") && !projectType.equals("JAVA_MAVEN")) {
            validationResultDisplayer.showValidationResult(new CheckstyleResult());
            return;
        }

        // Save all files
        ProjectMediator.getInstance().saveAllFiles();

        try {

            final Locale locale = TmcSettings.getDefault().getErrorMsgLocale();
            final ValidationResult result = new CheckstyleRunner(projectInfo.getProjectDirAsFile(), locale).run();

            validationResultDisplayer.showValidationResult(result);

        } catch (CheckstyleException exception) {
            ConvenientDialogDisplayer.getDefault().displayError("Checkstyle audit failed.");
            Exceptions.printStackTrace(exception);
        }
    }
}
