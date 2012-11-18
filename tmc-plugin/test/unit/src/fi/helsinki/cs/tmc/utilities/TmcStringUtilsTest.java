package fi.helsinki.cs.tmc.utilities;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class TmcStringUtilsTest {
    @Test
    public void testJoinCommaAndWithEmptyList() {
        assertEquals("", TmcStringUtils.joinCommaAnd(Arrays.asList()));
    }
    
    @Test
    public void testJoinCommaAndWithOneElement() {
        assertEquals("one", TmcStringUtils.joinCommaAnd(Arrays.asList("one")));
    }
    
    @Test
    public void testJoinCommaAndWithTwoElements() {
        assertEquals("one and two", TmcStringUtils.joinCommaAnd(Arrays.asList("one", "two")));
    }
    
    @Test
    public void testJoinCommaAndWithThreeElements() {
        assertEquals("one, two and three", TmcStringUtils.joinCommaAnd(Arrays.asList("one", "two", "three")));
    }
    
    @Test
    public void testJoinCommaAndWithFiveElements() {
        assertEquals("one, two, three, four and five", TmcStringUtils.joinCommaAnd(Arrays.asList("one", "two", "three", "four", "five")));
    }
}
