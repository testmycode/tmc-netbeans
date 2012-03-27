package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.utilities.zip.NbProjectUnzipper;
import java.io.File;
import org.apache.commons.io.FilenameUtils;

public class ExerciseUpdateOverwritingDecider implements NbProjectUnzipper.OverwritingDecider {
    private static final String s = File.separator;
    private TmcProjectFile projectFile;

    public ExerciseUpdateOverwritingDecider(File projectDir) {
        this.projectFile = TmcProjectFile.forProject(projectDir);
    }

    @Override
    public boolean mayOverwrite(String relPath) {
        return mayBothOverwriteAndDelete(relPath) ||
                !relPath.contains(s); // i.e. a file in the project's root dir
    }

    @Override
    public boolean mayDelete(String relPath) {
        return mayBothOverwriteAndDelete(relPath);
    }

    private boolean mayBothOverwriteAndDelete(String relPath) {
        return isNotAnExtraFile(relPath) && (
                relPath.startsWith("test") ||
                relPath.startsWith("lib") ||
                (relPath.startsWith("nbproject") && !relPath.startsWith("nbproject" + s + "private")) ||
                relPath.equals(".tmcproject.yml")
                );
    }

    private boolean isNotAnExtraFile(String relPath) {
        String normalized = FilenameUtils.normalizeNoEndSeparator(relPath, true);
        return !projectFile.getExtraStudentFiles().contains(normalized);
    }
}
