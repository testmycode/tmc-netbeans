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
    
    @Override
    public String getUsernameFieldName() {
        return "student number";
    }

    @Override
    public String getFirstRunMessage() {
        return "TestMyCode installed. Please fill in your student number in the next dialog.";
    }
}
