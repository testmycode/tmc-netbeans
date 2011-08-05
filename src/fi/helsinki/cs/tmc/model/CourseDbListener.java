package fi.helsinki.cs.tmc.model;

/**
 * Listens to changes in {@link CourseDb}.
 */
public interface CourseDbListener {
    /**
     * Called whenever the course DB is saved, i.e. after just about any change.
     */
    public void courseDbSaved();
}
