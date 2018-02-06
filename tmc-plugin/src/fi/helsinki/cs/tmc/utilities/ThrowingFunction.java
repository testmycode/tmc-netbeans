package fi.helsinki.cs.tmc.utilities;

@FunctionalInterface
public interface ThrowingFunction {
    void apply() throws Exception;
}
