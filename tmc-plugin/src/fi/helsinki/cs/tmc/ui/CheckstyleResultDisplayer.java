package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.stylerunner.CheckstyleResult;

public final class CheckstyleResultDisplayer {

    private static CheckstyleResultDisplayer instance;

    private CheckstyleResultDisplayer() {}

    public static CheckstyleResultDisplayer getInstance() {

        if (instance == null) {
            instance = new CheckstyleResultDisplayer();
        }

        return instance;
    }

    public void showCheckstyleResult(CheckstyleResult result) {

        TestResultWindow.get().setCheckstyleResult(result);
    }
}
