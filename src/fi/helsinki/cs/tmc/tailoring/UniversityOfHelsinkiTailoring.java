package fi.helsinki.cs.tmc.tailoring;

public class UniversityOfHelsinkiTailoring extends DefaultTailoring {
    @Override
    public String getDefaultServerUrl() {
        return ""; //TODO
    }

    @Override
    public String getDefaultUsername() {
        return getSystemUsername();
    }

    @Override
    public String getFirstRunMessage() {
        return "TestMyCode installed. Please check your username and select the course in the next dialog.";
    }
}
