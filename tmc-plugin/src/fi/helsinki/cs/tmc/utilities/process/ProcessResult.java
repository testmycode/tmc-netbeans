package fi.helsinki.cs.tmc.utilities.process;

public final class ProcessResult {
    public final int statusCode;
    public final String output;
    public final String errorOutput;

    public ProcessResult(int statusCode, String output, String errorOutput) {
        this.statusCode = statusCode;
        this.output = output;
        this.errorOutput = errorOutput;
    }
}
