package fi.helsinki.cs.tmc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark classes that have been refactored and brought under test.
 * This annotation will be removed after refactoring is complete.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Refactored {
    public String value() default "";
}
