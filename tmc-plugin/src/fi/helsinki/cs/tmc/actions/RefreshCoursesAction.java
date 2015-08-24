package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.data.CourseListUtils;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.NbTmcSettings;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcCoreSingleton;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListenerList;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Refreshes the course list in the background.
 */
public final class RefreshCoursesAction {

    private final static Logger log = Logger.getLogger(RefreshCoursesAction.class.getName());

    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private ConvenientDialogDisplayer dialogs;

    private BgTaskListenerList<List<Course>> listeners;

    private final TmcCore tmcCore;
    private final NbTmcSettings tmcSettings;

    public RefreshCoursesAction() {
        this(NbTmcSettings.getDefault());
    }

    /**
     * Default constructor.
     */
    public RefreshCoursesAction(NbTmcSettings settings) {
        this(settings, TmcCoreSingleton.getInstance());
    }

    /**
     * Dependency inject TmcCore for tests.
     */
    public RefreshCoursesAction(NbTmcSettings settings, TmcCore core) {
        this.tmcSettings = settings;
        this.serverAccess = new ServerAccess(settings);
        this.serverAccess.setSettings(settings);
        this.courseDb = CourseDb.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.listeners = new BgTaskListenerList<List<Course>>();

        this.tmcCore = core;
    }

    public RefreshCoursesAction addDefaultListener(boolean showDialogOnError, boolean updateCourseDb) {
        this.listeners.addListener(new DefaultListener(showDialogOnError, updateCourseDb));
        return this;
    }

    public RefreshCoursesAction addListener(BgTaskListener<List<Course>> listener) {
        this.listeners.addListener(listener);
        return this;
    }

    public void run() {
        CancellableCallable<List<Course>> courseListTask = new CancellableCallable<List<Course>>() {

            ListenableFuture<List<Course>> lf;
            @Override
            public List<Course> call() throws Exception {
                lf = tmcCore.listCourses();
                return lf.get();
            }

            @Override
            public boolean cancel() {
                return lf.cancel(true);
            }
        };

        BgTask.start("Refreshing course list", courseListTask, new BgTaskListener<List<Course>>() {

            @Override
            public void bgTaskReady(final List<Course> courses) {
                final Course currentCourseStub = CourseListUtils.getCourseByName(courses, courseDb.getCurrentCourseName());
                if (currentCourseStub != null) {
                    CancellableCallable<Course> currentCourseTask = new CancellableCallable<Course>() {

                        ListenableFuture<Course> lf;
                        @Override
                        public Course call() throws Exception {
                            lf = tmcCore.getCourse(currentCourseStub.getDetailsUrlAsUri());
                            return lf.get();
                        }

                        @Override
                        public boolean cancel() {
                            lf.cancel(true);
                            listeners.bgTaskCancelled();
                            return true;
                        }
                    };

                    BgTask.start("Loading course", currentCourseTask, new BgTaskListener<Course>() {
                        @Override
                        public void bgTaskReady(Course currentCourse) {
                            currentCourse.setExercisesLoaded(true);

                            ArrayList<Course> finalCourses = new ArrayList<Course>();
                            for (Course course : courses) {
                                if (course.getName().equals(currentCourse.getName())) {
                                    finalCourses.add(currentCourse);
                                } else {
                                    finalCourses.add(course);
                                }
                            }
                            listeners.bgTaskReady(finalCourses);
                        }

                        @Override
                        public void bgTaskCancelled() {
                            listeners.bgTaskCancelled();
                        }

                        @Override
                        public void bgTaskFailed(Throwable ex) {
                            log.log(Level.INFO, "Failed to download current course info.", ex);
                            listeners.bgTaskFailed(ex);
                        }
                    });

                } else {
                    listeners.bgTaskReady(courses);
                }
            }

            @Override
            public void bgTaskCancelled() {
                listeners.bgTaskCancelled();
            }

            @Override
            public void bgTaskFailed(Throwable ex) {
                log.log(Level.INFO, "Failed to download course list.", ex);
                listeners.bgTaskFailed(ex);
            }
        });
    }

    private class DefaultListener implements BgTaskListener<List<Course>> {

        private final boolean showDialogOnError;
        private final boolean updateCourseDb;

        public DefaultListener(boolean showDialogOnError, boolean updateCourseDb) {
            this.showDialogOnError = showDialogOnError;
            this.updateCourseDb = updateCourseDb;
        }

        @Override
        public void bgTaskReady(List<Course> result) {
            if (updateCourseDb) {
                courseDb.setAvailableCourses(result);
            }
        }

        @Override
        public void bgTaskCancelled() {
        }

        @Override
        public void bgTaskFailed(Throwable ex) {
            if (showDialogOnError) {
                dialogs.displayError("Course refresh failed.\n" + ServerErrorHelper.getServerExceptionMsg(ex));
            }
        }
    }
}