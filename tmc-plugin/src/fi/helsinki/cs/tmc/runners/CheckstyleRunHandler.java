package fi.helsinki.cs.tmc.runners;

import fi.helsinki.cs.tmc.stylerunner.CheckstyleRunner;

public class CheckstyleRunHandler {

    public void performAction() {
        CheckstyleRunner.check();
    }
}
