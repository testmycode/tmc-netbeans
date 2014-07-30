package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.utilities.zip.NbProjectUnzipper;
import java.io.File;
import org.apache.commons.io.FilenameUtils;

public class ExerciseUpdateOverwritingDecider implements NbProjectUnzipper.OverwritingDecider {

    private static final String s = File.separator;
    private final TmcProjectFile projectFile;

    public ExerciseUpdateOverwritingDecider(File projectDir) {
        this.projectFile = TmcProjectFile.forProject(projectDir);
    }

    @Override
    public boolean mayOverwrite(String relPath) {
        return mayBothOverwriteAndDelete(relPath) || isInProjectRootDir(relPath);
    }

    @Override
    public boolean mayDelete(String relPath) {
        return mayBothOverwriteAndDelete(relPath);
    }

    private boolean mayBothOverwriteAndDelete(String relPath) {
        return isNotAnExtraFile(relPath) && (
                relPath.startsWith("test") ||
                relPath.startsWith("src" + s + "test" + s + "java") ||
                relPath.startsWith("src" + s + "test" + s + "resources") ||
                relPath.startsWith("lib") ||
                (relPath.startsWith("nbproject") && !relPath.startsWith("nbproject" + s + "private")) ||
                relPath.equals(".tmcproject.yml") || relPath.equals(".tmcproject.json") ||
                relPath.endsWith("checkstyle.xml")
                );
    }

    private boolean isInProjectRootDir(String relPath) {
        return !relPath.contains(s);
    }

    private boolean isNotAnExtraFile(String relPath) {
        String normalized = FilenameUtils.normalizeNoEndSeparator(relPath, true);
        return !projectFile.getExtraStudentFiles().contains(normalized);
    }
}
