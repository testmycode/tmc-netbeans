package fi.helsinki.cs.tmc.model;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the contents of a {@code .tmcproject.yml} file.
 */
public class TmcProjectFile {

    private static final Logger log = Logger.getLogger(TmcProjectFile.class.getName());

    private List<String> extraStudentFiles;

    private TmcProjectFile() {
        this.extraStudentFiles = Collections.emptyList();
    }

    public List<String> getExtraStudentFiles() {
        return extraStudentFiles;
    }

    public void setExtraStudentFiles(List<String> extraStudentFiles) {
        this.extraStudentFiles = Collections.unmodifiableList(extraStudentFiles);
    }

    public static TmcProjectFile forProject(File projectDir) {
        try {
            File file = new File(projectDir.getPath() + File.separator + ".tmcproject.yml");
            return load(file);
        } catch (Exception e) {
            return getDefault();
        }
    }

    public static TmcProjectFile load(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getPath());
        }
        try {
            Reader reader =
                    new InputStreamReader(
                            new BufferedInputStream(new FileInputStream(file)),
                            Charset.forName("UTF-8"));
            try {
                Object root = new Yaml().load(reader);
                TmcProjectFile result = new TmcProjectFile();
                assignParsed(root, result);
                return result;
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            log.log(
                    Level.WARNING,
                    "Failed to read {0}: {1}",
                    new Object[] {file.getPath(), e.getMessage()});
            throw e;
        }
    }

    private static TmcProjectFile getDefault() {
        return new TmcProjectFile();
    }

    private static void assignParsed(Object root, TmcProjectFile result) {
        if (!(root instanceof Map)) {
            return;
        }
        Map<?, ?> rootMap = (Map<?, ?>) root;
        Object files = rootMap.get("extra_student_files");
        if (files instanceof List) {
            List<String> extraStudentFiles = new ArrayList<String>();
            for (Object value : (List<?>) files) {
                if (value instanceof String) {
                    extraStudentFiles.add((String) value);
                }
            }
            result.setExtraStudentFiles(extraStudentFiles);
        }
    }
}
