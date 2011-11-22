package fi.helsinki.cs.tmc.functionaltests.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Semaphore;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpRequestHandlerResolver;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

/**
 * A HTTP server running on a random port in a single background thread.
 * 
 * An exception in a request handler will cause the server to shut down.
 * The exception will be propagated when {@link #stop()} is called.
 */
public class AdHocHttpServer {
    private HttpRequestHandlerResolver handlers;
    
    private ServerSocket serverSocket;
    private HttpService httpService;
    private Thread thread;
    
    private Exception inThreadException; // Set by thread, read in stop()
    private Semaphore requestCounter = new Semaphore(0);

    public AdHocHttpServer() {
        this.handlers = new HttpRequestHandlerRegistry();
    }
    
    public void setHandler(HttpRequestHandler handler) {
        HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
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
        HttpProcessor proc = new ImmutableHttpProcessor(new HttpResponseInterceptor[] {
            new ResponseDate(),
            new ResponseServer(),
            new ResponseContent(),
            new ResponseConnControl()
        });
        
        httpService = new HttpService(
                proc,
                new DefaultConnectionReuseStrategy(),
                new DefaultHttpResponseFactory(),
                handlers,
                new SyncBasicHttpParams()
                );
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
            throw new IllegalStateException("Not started");
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
                try {
                    debug("Accepting at port " + serverSocket.getLocalPort());
                    Socket socket;
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
                    DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    conn.bind(socket, httpService.getParams());
                    HttpContext ctx = new BasicHttpContext(null);
                    while (!Thread.currentThread().isInterrupted() && conn.isOpen()) {
                        httpService.handleRequest(conn, ctx);
                        requestCounter.release();
                    }
                    debug("Connection processed");
                } catch (ConnectionClosedException ex) {
                    // No problem I think
                } catch (InterruptedIOException ex) {
                    debug("InterruptedIOException: " + ex);
                    break;
                } catch (Exception ex) {
                    inThreadException = ex;
                    debug("Exception: " + ex);
                    break;
                }
            }
        }
    };
    
    protected void debug(String msg) {
        System.out.println("AdHocHttpServer: " + msg);
    }
}
