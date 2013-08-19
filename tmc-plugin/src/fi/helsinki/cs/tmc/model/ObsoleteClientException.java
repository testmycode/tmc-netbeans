package fi.helsinki.cs.tmc.model;

public  class ObsoleteClientException extends Exception {
    public ObsoleteClientException() {
        super("Please update the TMC plugin.\nUse Help -> Check for Updates.");
    }
}
