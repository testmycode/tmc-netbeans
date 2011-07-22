package fi.helsinki.cs.tmc.tailoring;

public class Tailorings {
    public static Tailoring getCurrent() {
        //TODO: load from a file somewhere.
        return new DefaultTailoring();
    }
}
