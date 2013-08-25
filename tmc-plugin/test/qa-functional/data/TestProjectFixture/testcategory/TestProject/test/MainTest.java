
import fi.helsinki.cs.tmc.testrunner.Points;
import org.junit.Test;
import static org.junit.Assert.*;

public class MainTest {
    
    @Test
    @Points("addpoint")
    public void testAdd() {
        assertEquals(9, Main.add(7, 2));
    }
    
    @Test
    @Points("addpoint")
    public void testSub() {
        assertEquals(5, Main.sub(7, 2));
    }
}
