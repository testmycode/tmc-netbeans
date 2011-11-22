package fi.helsinki.cs.tmc.utilities;

import java.util.concurrent.Callable;

/**
 * Like {@link Callable}, but takes a parameter.
 * 
 * @param <P> The parameter type.
 * @param <R> The return type.
 */
public interface ParamCallable<P, R> {
    public R call(P param) throws Exception;
}
