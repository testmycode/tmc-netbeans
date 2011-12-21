package fi.helsinki.cs.tmc.tailoring;

public class UniversityOfHelsinkiTailoring extends DefaultTailoring {
    @Override
    public String getDefaultServerUrl() {
        return "http://tmc.cs.helsinki.fi";
    }

    @Override
    public String getDefaultUsername() {
        return "";
    }
}
