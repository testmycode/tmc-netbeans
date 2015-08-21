package fi.helsinki.cs.tmc.runners;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import fi.helsinki.cs.tmc.data.ResultCollector;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.model.NBTmcSettings;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.stylerunner.validation.CheckstyleResult;
import fi.helsinki.cs.tmc.stylerunner.validation.ValidationResult;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;
import java.nio.file.Paths;

import javax.swing.SwingUtilities;

import org.netbeans.api.project.Project;

import org.openide.util.Exceptions;

public final class CheckstyleRunHandler {

    private Project project;
    private final ConvenientDialogDisplayer dialogDisplayer = ConvenientDialogDisplayer.getDefault();
    private ValidationResult validationResult = new CheckstyleResult();

    public void performAction(final ResultCollector resultCollector, final Project project) {
        this.project = project;
        final TmcProjectInfo projectInfo = ProjectMediator.getInstance().wrapProject(project);
        final String projectType = projectInfo.getProjectType().name();
        ProjectMediator.getInstance().saveAllFiles();
        try {
        ListenableFuture<ValidationResult> result = TmcCoreSingleton.getInstance().runCheckstyle(
                Paths.get(
                        projectInfo.getProjectDirAsFile().getAbsolutePath()
                ));
        Futures.addCallback(result, new ExplainValidationResult(resultCollector, dialogDisplayer));
        } catch (TmcCoreException ex) {
            ConvenientDialogDisplayer.getDefault().displayError("Checkstyle audit failed.");
            Exceptions.printStackTrace(ex);
        }
    }

}

class ExplainValidationResult implements FutureCallback<ValidationResult> {

    ResultCollector resultCollector;
    ConvenientDialogDisplayer dialogDisplayer;

    public ExplainValidationResult(ResultCollector resultCollector, ConvenientDialogDisplayer dialogDisplayer) {
        this.resultCollector = resultCollector;
        this.dialogDisplayer = dialogDisplayer;
    }

    @Override
    public void onSuccess(final ValidationResult v) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                resultCollector.setValidationResult(v);
            }
        });
    }

    @Override
    public void onFailure(Throwable thrwbl) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dialogDisplayer.displayError("Failed to validate the code.");
            }
        });
    }
}
