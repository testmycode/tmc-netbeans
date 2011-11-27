package fi.helsinki.cs.tmc.utilities.http;

import java.util.concurrent.Future;
import fi.helsinki.cs.tmc.testing.AdHocHttpServer;
import fi.helsinki.cs.tmc.testing.MockBgTaskListener;
import fi.helsinki.cs.tmc.utilities.BgTask;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class HttpRequestExecutorTest {
    
    private AdHocHttpServer server;
    
    @Before
    public void setUp() throws Exception {
        server = new AdHocHttpServer();
    }
    
    @After
    public void tearDown() throws Exception {
        server.stop();
    }
    
    @Test
    public void testDirectReturn() throws Exception {
        server.setHandler(oneTwoThreeHandler());
        server.start();
        
        byte[] result = new HttpRequestExecutor(server.getBaseUrl()).call();
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }
    
    @Test
    public void testMultipleCalls() throws Exception {
        server.setHandler(oneTwoThreeHandler());
        server.start();
        
        MockBgTaskListener<byte[]> listener1 = new MockBgTaskListener<byte[]>();
        MockBgTaskListener<byte[]> listener2 = new MockBgTaskListener<byte[]>();
        MockBgTaskListener<byte[]> listener3 = new MockBgTaskListener<byte[]>();
        Future<byte[]> result1 = BgTask.start("1", listener1, new HttpRequestExecutor(server.getBaseUrl()));
        Future<byte[]> result2 = BgTask.start("2", listener2, new HttpRequestExecutor(server.getBaseUrl()));
        Future<byte[]> result3 = BgTask.start("3", listener3, new HttpRequestExecutor(server.getBaseUrl()));
        
        assertArrayEquals(new byte[] { 1, 2, 3 }, result3.get());
        assertArrayEquals(new byte[] { 1, 2, 3 }, result2.get());
        assertArrayEquals(new byte[] { 1, 2, 3 }, result1.get());
        listener1.waitForCall();
        listener2.waitForCall();
        listener3.waitForCall();
        listener1.assertGotSuccess();
        listener2.assertGotSuccess();
        listener3.assertGotSuccess();
    }
    
    @Test
    public void testFollowingRedirectsAutomatically() throws Exception {
        final AtomicBoolean redirected = new AtomicBoolean(false);
        server.setHandler(new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest req, HttpResponse res, HttpContext hc) throws HttpException, IOException {
                if (req.getRequestLine().getUri().startsWith("/one")) {
                    res.setStatusCode(302);
                    res.addHeader("Location", server.getBaseUrl() + "/two");
                    redirected.set(true);
                } else {
                    res.setEntity(byteEntity(new byte[] { 4, 5, 6 }));
                }
            }
        });
        server.start();
        
        byte[] result = new HttpRequestExecutor(server.getBaseUrl() + "/one").call();
        assertArrayEquals(new byte[] { 4, 5, 6 }, result);
        assertTrue(redirected.get());
    }
    
    private HttpRequestHandler oneTwoThreeHandler() {
        return new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest req, HttpResponse res, HttpContext hc) throws HttpException, IOException {
                res.setEntity(byteEntity(new byte[] { 1, 2, 3 }));
            }
        };
    }
    
    private HttpEntity byteEntity(byte[] bytes) {
        ByteArrayEntity ent = new ByteArrayEntity(bytes);
        ent.setContentType("application/data");
        return ent;
    }
}
