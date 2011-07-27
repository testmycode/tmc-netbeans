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
}
