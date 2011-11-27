package fi.helsinki.cs.tmc.utilities;

import org.junit.Test;
import static org.junit.Assert.*;
import static fi.helsinki.cs.tmc.utilities.UriUtils.*;

public class UriUtilsTest {
    @Test
    public void testSettingQueryParameters() {
        String base = "http://example.com/one/two";
        
        assertEquals("http://example.com/one/two?foo=bar", withQueryParam(base, "foo", "bar"));
        assertEquals("http://example.com/one/two?foo=bar", withQueryParam(base + "?", "foo", "bar"));
        assertEquals("http://example.com/one/two?foo=bar", withQueryParam(base + "?foo=xoo", "foo", "bar"));
        
        assertEquals("http://example.com/one/two?xoo=xoo&foo=bar", withQueryParam(base + "?xoo=xoo", "foo", "bar"));
        
        assertEquals("http://example.com/one/two?foo=a+b+c", withQueryParam(base, "foo", "a b c"));
        
        assertEquals("http://example.com/one/two?foo=b%25C3%25B6%25C3%25B6", withQueryParam(base, "foo", "böö"));
    }
}
