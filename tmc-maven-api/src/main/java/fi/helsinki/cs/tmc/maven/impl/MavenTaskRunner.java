package fi.helsinki.cs.tmc.maven.impl;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.maven.cli.MavenCli;

public class MavenTaskRunner implements Runnable {
    private File dir;
    private String[] goals;
    private Map<String, String> props;
    private PrintStream stdoutStream;
    private PrintStream stderrStream;
    private int exitCode;

    public MavenTaskRunner(File dir, String[] goals, Map<String, String> props, Writer stdoutWriter, Writer stderrWriter) {
        this.dir = dir;
        this.goals = goals;
        this.props = props;
        this.stdoutStream = writerToPrintStream(stdoutWriter);
        this.stderrStream = writerToPrintStream(stderrWriter);
    }
    
    private static PrintStream writerToPrintStream(Writer writer) {
        Charset cs = Charset.defaultCharset();
        OutputStream os = new WriterOutputStream(writer, cs, 1024, true);
        return new PrintStream(os, true);
    }
    
    @Override
    public void run() {
        MavenCli mc = new MavenCli();
        exitCode = mc.doMain(getArgs(), dir.getPath(), stdoutStream, stderrStream);
    }

    /**
     * Returns the exit code, available after the thread exits.
     */
    public int getExitCode() {
        return exitCode;
    }

    private String[] getArgs() {
        String[] args = new String[props.size() + goals.length];
        int i = 0;
        for (String key : props.keySet()) {
            args[i++] = "-D" + key + "=" + props.get(key);
        }
        System.arraycopy(goals, 0, args, i, goals.length);
        return args;
    }
    
}
