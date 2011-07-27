package fi.helsinki.cs.tmc.tailoring;

public class UniversityOfHelsinkiTailoring extends DefaultTailoring {
    @Override
    public String getDefaultServerUrl() {
        return ""; //TODO
    }

    @Override
    public String getDefaultUsername() {
        String user = System.getenv("USER"); // Unix
        if (user == null) {
            user = System.getenv("USERNAME"); // Windows
        }
        if (user == null) {
            user = "";
        }
        return user;
    }
}
