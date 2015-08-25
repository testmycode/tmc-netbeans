package fi.helsinki.cs.tmc.testing;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerMapper;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

import org.openide.util.Exceptions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Semaphore;

public class AdHocHttpServer {

    /**
     * A HTTP server running on a random port in a single background thread.
     *
     * An exception in a request handler will cause the server to shut down. The
     * exception will be propagated when {@link #stop()} is called.
     */
    private HttpRequestHandlerMapper handlers;

    private ServerSocket serverSocket;
    private HttpService httpService;
    private Thread thread;

    private Exception inThreadException; // Set by thread, read in stop()
    private Semaphore requestCounter = new Semaphore(0);

    private volatile boolean debugEnabled = false;

    public AdHocHttpServer() {
        this.handlers = new UriHttpRequestHandlerMapper();
    }

    public void enableDebug() {
        debugEnabled = true;
    }

    public void setHandler(HttpRequestHandler handler) {
        UriHttpRequestHandlerMapper registry = new UriHttpRequestHandlerMapper();
        registry.register("*", handler);
        this.handlers = registry;
    }

    public boolean isStarted() {
        return thread != null;
    }

    public int getPort() {
        if (!isStarted()) {
            throw new IllegalStateException("Server must be started first");
        }
        return serverSocket.getLocalPort();
    }

    public String getBaseUrl() {
        return "http://localhost:" + getPort();
    }

    public synchronized void start() throws IOException {
        if (isStarted()) {
            throw new IllegalStateException("Already started");
        }

        setupServerSocket();
        setupHttpClientIncantations();
        startThread();
        debug("Started");
    }

    private void setupServerSocket() throws IOException {
        serverSocket = new ServerSocket();
        InetSocketAddress addr = new InetSocketAddress("localhost", 0);
        serverSocket.setReceiveBufferSize(64);
        serverSocket.bind(addr);
    }

    private void setupHttpClientIncantations() {
        HttpProcessor proc = new ImmutableHttpProcessor(new HttpResponseInterceptor[]{
            new ResponseDate(),
            new ResponseServer(),
            new ResponseContent(),
            new ResponseConnControl()
        });

        httpService = new HttpService(
                proc,
                handlers);
    }

    private void startThread() {
        thread = new Thread(inThread, "FakeServer");
        thread.setDaemon(true);
        thread.start();
    }

    public void waitForRequestToComplete() throws Exception {
        requestCounter.acquire();
    }

    public synchronized void stop() throws Exception {
        if (!isStarted()) {
            return;
        }

        debug("Stopping");
        thread.interrupt();
        serverSocket.close();
        thread.join();
        debug("Stopped");

        if (inThreadException != null) {
            throw inThreadException;
        }
    }

    private Runnable inThread = new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                Socket socket = null;
                try {
                    debug("Accepting at port " + serverSocket.getLocalPort());
                    try {
                        socket = serverSocket.accept();
                    } catch (SocketException ex) {
                        if (Thread.interrupted()) {
                            break;
                        } else {
                            throw ex;
                        }
                    }

                    debug("Got connection");
                    DefaultBHttpServerConnection conn = new DefaultBHttpServerConnection(5000);
                    conn.bind(socket);
                    HttpContext ctx = new BasicHttpContext(null);
                    while (!Thread.currentThread().isInterrupted() && conn.isOpen()) {
                        httpService.handleRequest(conn, ctx);
                        requestCounter.release();
                    }
                    debug("Connection processed");
                } catch (ConnectionClosedException ex) {
                    // No problem I think
                } catch (Exception ex) {
                    inThreadException = ex;
                    debug("Exception: " + ex);
                    break;
                } finally {
                    try {
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        }
    };

    protected void debug(Object msg) {
        if (debugEnabled) {
            System.out.println(this.getClass().getSimpleName() + ": " + msg.toString());
        }
    }
}