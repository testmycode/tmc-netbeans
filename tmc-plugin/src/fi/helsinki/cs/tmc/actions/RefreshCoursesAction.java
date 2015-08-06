package fi.helsinki.cs.tmc.actions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import hy.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.data.CourseListUtils;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.model.NBTmcSettings;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.FutureCallbackList;
import hy.tmc.core.TmcCore;
import hy.tmc.core.exceptions.TmcCoreException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;

/**
 * Refreshes the course list in the background.
 */
public final class RefreshCoursesAction {

    private final static Logger log = Logger.getLogger(RefreshCoursesAction.class.getName());

    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private ConvenientDialogDisplayer dialogs;

    private FutureCallbackList<List<Course>> callbacks;
    private final TmcCore tmcCore;
    private final NBTmcSettings tmcSettings;

    public RefreshCoursesAction() {
        this(NBTmcSettings.getDefault());
    }

    /**
     * Default constructor.
     */
    public RefreshCoursesAction(NBTmcSettings settings) {
        this(settings, TmcCoreSingleton.getInstance());
    }

    /**
     * Dependency inject TmcCore for tests.
     */
    public RefreshCoursesAction(NBTmcSettings settings, TmcCore core) {
        this.tmcSettings = settings;
        this.serverAccess = new ServerAccess(settings);
        this.serverAccess.setSettings(settings);
        this.courseDb = CourseDb.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.callbacks = new FutureCallbackList<List<Course>>();
        this.tmcCore = core;
    }

    public RefreshCoursesAction addDefaultListener(boolean showDialogOnError, boolean updateCourseDb) {
        this.callbacks.addListener(new DefaultListener(showDialogOnError, updateCourseDb));
        return this;
    }

    public RefreshCoursesAction addListener(FutureCallback<List<Course>> callback) {
        this.callbacks.addListener(callback);
        return this;
    }

    /**
     * Starts downloading course-jsons from TMC-server. Url of TMC-server is
     * defined in TmcSettings object. TmcCore includes all logic, callbacks here
     * are run after core-futures are ready.
     */
    public void run() {
        try {
            if (settingsAreSet()) {
                ProgressHandle courseRefresh = ProgressHandleFactory.createSystemHandle(
                    "Refreshing course list");
                courseRefresh.start();
                System.err.println("URLI: " + tmcSettings.getServerAddress());
                ListenableFuture<List<Course>> listCourses = this.tmcCore.listCourses(tmcSettings);
                Futures.addCallback(listCourses, new LoadCourses(courseRefresh));
            }
        } catch (TmcCoreException ex) {
            Exceptions.printStackTrace(ex);
            callbacks.onFailure(ex);
        }
    }

    private boolean settingsAreSet() {
        return tmcSettings.userDataExists()
                && tmcSettings.getServerAddress() != null
                && !tmcSettings.getServerAddress().trim().isEmpty();
    }

    class LoadCourses implements FutureCallback<List<Course>> {

        private ProgressHandle lastAction;

        /**
         * This callBack is run when ListenableFuture (to witch this is
         * attached) is done. On success method takes list of Course-objects,
         * searches the current course and starts uploading the details of the
         * course. If no currentCourse found, no need to update details.
         *
         * @param ProgressHandle shows to user that action is processing.
         */
        public LoadCourses(ProgressHandle lastAction) {
            this.lastAction = lastAction;
        }

        @Override
        public void onSuccess(final List<Course> courses) {
            lastAction.finish();
            Course currentCourse = CourseListUtils.getCourseByName(
                    courses, courseDb.getCurrentCourseName()
            );
            if (currentCourse != null) {
                try {
                    ProgressHandle loadingCourse = ProgressHandleFactory.
                            createSystemHandle("Loading course");
                    loadingCourse.start();
                    ListenableFuture<Course> courseFuture = tmcCore.getCourse(
                            tmcSettings, currentCourse.getDetailsUrl()
                    );
                    Futures.addCallback(courseFuture, new UpdateCourse(courses, loadingCourse));
                } catch (TmcCoreException ex) {
                    Exceptions.printStackTrace(ex);
                    callbacks.onFailure(ex);
                }
            } else {
                callbacks.onSuccess(courses);
            }
        }

        @Override
        public void onFailure(Throwable ex) {
            lastAction.finish();
            log.log(Level.INFO, "Failed to download current course info.", ex);
            callbacks.onFailure(ex);
        }
    }

    /**
     * When detailed current course is present, courses will be given to
     * FutureCallbackList, that shares the result to every callback that is
     * attached to that list.
     */
    class UpdateCourse implements FutureCallback<Course> {

        private List<Course> courses;
        private ProgressHandle lastAction;

        public UpdateCourse(List<Course> courses, ProgressHandle lastAction) {
            this.courses = courses;
            this.lastAction = lastAction;
        }

        @Override
        public void onSuccess(Course detailedCourse) {
            lastAction.finish();
            detailedCourse.setExercisesLoaded(true);
            ArrayList<Course> finalCourses = new ArrayList<Course>();
            for (Course course : courses) {
                if (course.getName().equals(detailedCourse.getName())) {
                    finalCourses.add(detailedCourse);
                } else {
                    finalCourses.add(course);
                }
            }
            callbacks.onSuccess(finalCourses);
        }

        @Override
        public void onFailure(Throwable ex) {
            lastAction.finish();
            log.log(Level.INFO, "Failed to download current course info.", ex);
            callbacks.onFailure(ex);
        }
    }

    /**
     * Updates the courseDb after all course-jsons are downloaded.
     */
    private class DefaultListener implements FutureCallback<List<Course>> {

        private final boolean showDialogOnError;
        private final boolean updateCourseDb;

        public DefaultListener(boolean showDialogOnError, boolean updateCourseDb) {
            this.showDialogOnError = showDialogOnError;
            this.updateCourseDb = updateCourseDb;
        }

        @Override
        public void onSuccess(final List<Course> result) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (updateCourseDb) {
                        courseDb.setAvailableCourses(result);
                    }
                }

            });
        }

        @Override
        public void onFailure(final Throwable ex) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (showDialogOnError) {
                        dialogs.displayError("Course refresh failed.\n" + ServerErrorHelper.getServerExceptionMsg(ex));
                    }
                }

            });
        }
    }
}
