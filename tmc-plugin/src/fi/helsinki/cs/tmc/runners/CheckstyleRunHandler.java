package fi.helsinki.cs.tmc.runners;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import fi.helsinki.cs.tmc.stylerunner.CheckstyleRunner;
import java.io.File;
import java.util.ArrayList;
import org.openide.util.Exceptions;

public class CheckstyleRunHandler {

    public void performAction() {
        try {
            new CheckstyleRunner(new ArrayList<File>()).run();
        } catch (CheckstyleException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
