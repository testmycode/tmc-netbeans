package fi.helsinki.cs.tmc.utilities.process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.netbeans.api.extexecution.ExternalProcessSupport;
import org.openide.filesystems.FileUtil;
import org.openide.windows.InputOutput;

/**
 * Used to run subprocesses with a timeout and capture their output.
 *
 * <p>
 * TODO: make cancellable
 */
@Deprecated
public class ProcessRunner implements Callable<ProcessResult> {
    private static final String PROCESS_TREE_IDENTIFIER_NAME = "PROCESS_TREE_IDENTIFIER_FOR_NB";

    private final String[] command;
    private final File workDir;
    private final InputOutput inOut;

    public ProcessRunner(String[] command, File workDir, InputOutput inOut) {
        this.command = command;
        this.workDir = workDir;
        this.inOut = inOut;
    }

    @Override
    public ProcessResult call() throws Exception {
        String processTreeIdentifier = UUID.randomUUID().toString();

        @SuppressWarnings("unchecked")
        String[] envp = makeEnvp(System.getenv(), Collections.singletonMap(PROCESS_TREE_IDENTIFIER_NAME, processTreeIdentifier));

        Process process = Runtime.getRuntime().exec(command, envp, workDir);

        int statusCode;
        ByteArrayOutputStream stdoutBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBuf = new ByteArrayOutputStream();

        OutputStream out;
        OutputStream err;
        if (inOut != null) {
            out = new TeeOutputStream(stdoutBuf, new WriterOutputStream(inOut.getOut()));
            err = new TeeOutputStream(stdoutBuf, new WriterOutputStream(inOut.getErr()));
        } else {
            out = stdoutBuf;
            err = stderrBuf;
        }

        try {
            startReaderThread(process.getInputStream(), out);
            startReaderThread(process.getErrorStream(), err);

            statusCode = process.waitFor();
        } catch (InterruptedException e) {
            Map<String, String> destroyEnv = Collections.singletonMap(PROCESS_TREE_IDENTIFIER_NAME, processTreeIdentifier);
            ExternalProcessSupport.destroy(process, destroyEnv);
            throw e;
        }

        return new ProcessResult(statusCode, stdoutBuf.toString("UTF-8"), stderrBuf.toString("UTF-8"));
    }

    private String[] makeEnvp(Map<String, String>... envs) {
        int totalEntries = 0;
        for (Map<String, String> env : envs) {
            totalEntries += env.size();
        }

        String[] envp = new String[totalEntries];
        int i = 0;
        for (Map<String, String> env : envs) {
            for (Map.Entry<String, String> envEntry : env.entrySet()) {
                envp[i++] = envEntry.getKey() + "=" + envEntry.getValue();
            }
        }

        return envp;
    }

    private Thread startReaderThread(final InputStream is, final OutputStream os) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    FileUtil.copy(is, os);
                } catch (IOException e) {
                }
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

}
