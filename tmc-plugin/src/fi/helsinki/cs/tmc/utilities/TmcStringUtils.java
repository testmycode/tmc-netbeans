package fi.helsinki.cs.tmc.utilities;

import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class TmcStringUtils {
    /**
     * Returns a comma separated list, but the last comma is an "and".
     */
    public static <T> String joinCommaAnd(List<T> objs) {
        int size = objs.size();
        if (size == 0) {
            return "";
        } else if (size == 1) {
            return objs.get(0).toString();
        } else {
            StringBuilder sb = new StringBuilder();
            Iterator<T> iter = objs.iterator();
            for (int i = 0; i < size - 2; ++i) {
                sb.append(iter.next()).append(", ");
            }
            sb.append(iter.next()).append(" and ").append(iter.next());
            assert !iter.hasNext();
            return sb.toString();
        }
    }
}
