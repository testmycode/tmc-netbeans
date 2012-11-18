package fi.helsinki.cs.tmc.utilities;

import java.util.HashMap;

public class Inflector {
    private static final HashMap<String, String> specialPlurals;
    static {
        specialPlurals = new HashMap<String, String>();
        specialPlurals.put("is", "are");
    }
    
    /**
     * pluralize(1, "foo") -> "foo"; pluralize(3, "foo") -> "foos" plus some special cases.
     */
    public static String pluralize(int count, String name) {
        if (count == 1) {
            return name;
        } else {
            if (name.startsWith("a ")) {
                name = name.substring(2);
            } else if (name.startsWith("an ")) {
                name = name.substring(3);
            }
            
            String special = specialPlurals.get(name);
            if (special != null) {
                return special;
            } else {
                if (name.endsWith("s")) {
                    return name + "es";
                } else {
                    return name + "s";
                }
            }
        }
    }
}
