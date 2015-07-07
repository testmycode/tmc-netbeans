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
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import fi.helsinki.cs.tmc.utilities.FutureCallbackList;
import hy.tmc.core.TmcCore;
import hy.tmc.core.exceptions.TmcCoreException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Exceptions;

/**
 * Refreshes the course list in the background.
 */
public final class RefreshCoursesAction {

    private final static Logger log = Logger.getLogger(RefreshCoursesAction.class.getName());

    private ServerAccess serverAccess;
    private CourseDb courseDb;
    private ConvenientDialogDisplayer dialogs;

    //private BgTaskListenerList<List<Course>> listeners;
    private FutureCallbackList<List<Course>> callbacks;
    private final TmcCore tmcCore;
    private final NBTmcSettings tmcSettings;

    public RefreshCoursesAction() {
        this(NBTmcSettings.getDefault());
    }

    public RefreshCoursesAction(NBTmcSettings settings) {
        this.tmcSettings = settings;
        this.serverAccess = new ServerAccess(settings);
        this.serverAccess.setSettings(settings);
        this.courseDb = CourseDb.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();

        //this.listeners = new BgTaskListenerList<List<Course>>();
        this.callbacks = new FutureCallbackList<List<Course>>();
        this.tmcCore = TmcCoreSingleton.getInstance();
    }

    public RefreshCoursesAction addDefaultListener(boolean showDialogOnError, boolean updateCourseDb) {
        this.callbacks.addListener(new DefaultListener(showDialogOnError, updateCourseDb));
        return this;
    }

    public RefreshCoursesAction addListener(FutureCallback<List<Course>> callbacks) {
        this.callbacks.addListener(callbacks);
        return this;
    }

    //HUOM metodissa oldRun on vanha toteutus. Se hakee palvelmelta nykyisen kurssin "fullcourse info".
    public void run() {
        try {
            ListenableFuture<List<Course>> listCourses = this.tmcCore.listCourses(tmcSettings);
            Futures.addCallback(listCourses, new FutureCallback<List<Course>>() {
                @Override
                public void onSuccess(final List<Course> courses) {
                    Course currentCourse = CourseListUtils.getCourseByName(courses, courseDb.getCurrentCourseName());
                    if (currentCourse != null) {
                        try {
                            System.out.println("DETAILS URL: " + currentCourse.getDetailsUrl());
                            ListenableFuture<Course> courseFuture = tmcCore.getCourse(tmcSettings, currentCourse.getDetailsUrl());
                            Futures.addCallback(courseFuture, new FutureCallback<Course>() {
                                @Override
                                public void onSuccess(Course detailedCourse) {
                                    detailedCourse.setExercisesLoaded(true);
                                    ArrayList<Course> finalCourses = new ArrayList<Course>();
                                    for(Course course: courses){
                                        if(course.getName().equals(detailedCourse.getName())) {
                                            finalCourses.add(detailedCourse);
                                        } else {
                                            finalCourses.add(course);
                                        }
                                    }
                                    callbacks.onSuccess(finalCourses);
                                }
                                
                                @Override
                                public void onFailure(Throwable ex) {
                                    log.log(Level.INFO, "Failed to download current course info.", ex);
                                    callbacks.onFailure(ex);
                                }
                            });
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
                    log.log(Level.INFO, "Failed to download current course info.", ex);
                    callbacks.onFailure(ex);
                }
            }
            );
        } catch (TmcCoreException ex) {
            Exceptions.printStackTrace(ex);
            callbacks.onFailure(ex);
        }
    }

//    public void run() {
//        CancellableCallable<List<Course>> courseListTask = serverAccess.getDownloadingCourseListTask();
//        BgTask.start("Refreshing course list", courseListTask, new BgTaskListener<List<Course>>() {
//
//            @Override
//            public void bgTaskReady(final List<Course> courses) {
//                Course currentCourseStub = CourseListUtils.getCourseByName(courses, courseDb.getCurrentCourseName());
//                // CurrentCourseStub on null jos on vanhentunut/ ei enää olemassa servulla
//                if (currentCourseStub != null) {
//                    // Jos on olemassa servulla, päivitetään lokaali kurssi servun kurssia vastaavaksi
//                    CancellableCallable<Course> currentCourseTask = serverAccess.getFullCourseInfoTask(currentCourseStub);
//
//                    BgTask.start("Loading course", currentCourseTask, new BgTaskListener<Course>() {
//                        @Override
//                        public void bgTaskReady(Course currentCourse) {
//                            currentCourse.setExercisesLoaded(true);
//
//                            ArrayList<Course> finalCourses = new ArrayList<Course>();
//                            for (Course course : courses) {
//                                if (course.getName().equals(currentCourse.getName())) {
//                                    finalCourses.add(currentCourse);
//                                } else {
//                                    finalCourses.add(course);
//                                }
//                            }
//                            listeners.bgTaskReady(finalCourses);
//                        }
//
//                        @Override
//                        public void bgTaskCancelled() {
//                            listeners.bgTaskCancelled();
//                        }
//
//                        @Override
//                        public void bgTaskFailed(Throwable ex) {
//                            log.log(Level.INFO, "Failed to download current course info.", ex);
//                            listeners.bgTaskFailed(ex);
//                        }
//                    });
//                } else {
//                    listeners.bgTaskReady(courses);
//                }
//            }
//
//            @Override
//            public void bgTaskCancelled() {
//                listeners.bgTaskCancelled();
//            }
//
//            @Override
//            public void bgTaskFailed(Throwable ex) {
//                log.log(Level.INFO, "Failed to download course list.", ex);
//                listeners.bgTaskFailed(ex);
//            }
//        });
//    }

    private class DefaultListener implements FutureCallback<List<Course>> {

        private final boolean showDialogOnError;
        private final boolean updateCourseDb;

        public DefaultListener(boolean showDialogOnError, boolean updateCourseDb) {
            this.showDialogOnError = showDialogOnError;
            this.updateCourseDb = updateCourseDb;
        }

//        @Override
//        public void bgTaskReady(List<Course> result) {
//            if (updateCourseDb) {
//                courseDb.setAvailableCourses(result);
//            }
//        }
//
//        @Override
//        public void bgTaskCancelled() {
//        }
//
//        @Override
//        public void bgTaskFailed(Throwable ex) {
//            if (showDialogOnError) {
//                dialogs.displayError("Course refresh failed.\n" + ServerErrorHelper.getServerExceptionMsg(ex));
//            }
//        }

        @Override
        public void onSuccess(List<Course> result) {
            if (updateCourseDb) {
                courseDb.setAvailableCourses(result);
            }
        }

        @Override
        public void onFailure(Throwable ex) {
            if (showDialogOnError) {
                dialogs.displayError("Course refresh failed.\n" + ServerErrorHelper.getServerExceptionMsg(ex));
            }
        }
    }
}