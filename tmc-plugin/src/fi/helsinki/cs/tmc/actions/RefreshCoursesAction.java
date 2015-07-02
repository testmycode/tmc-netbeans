package fi.helsinki.cs.tmc.actions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import hy.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.data.CourseListUtils;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ServerAccess;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListenerList;
import fi.helsinki.cs.tmc.utilities.CancellableCallable;
import hy.tmc.core.TmcCore;
import hy.tmc.core.exceptions.TmcCoreException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    
    private BgTaskListenerList<List<Course>> listeners;

    public RefreshCoursesAction() {
        this(TmcSettings.getDefault());
    }
    
    public RefreshCoursesAction(TmcSettings settings) {
        this.serverAccess = new ServerAccess(settings);
        this.serverAccess.setSettings(settings);
        this.courseDb = CourseDb.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();

        this.listeners = new BgTaskListenerList<List<Course>>();
    }

    public RefreshCoursesAction addDefaultListener(boolean showDialogOnError, boolean updateCourseDb) {
        this.listeners.addListener(new DefaultListener(showDialogOnError, updateCourseDb));
        return this;
    }

    public RefreshCoursesAction addListener(BgTaskListener<List<Course>> listener) {
        this.listeners.addListener(listener);
        return this;
    }

    public void run() throws TmcCoreException {
        CancellableCallable<List<Course>> courseListTask = serverAccess.getDownloadingCourseListTask();
        TmcCore c = new TmcCore();
        final ListenableFuture<List<Course>> courses = c.listCourses();
        /*
        
        courses.addListener(new Runnable() {

            @Override
            public void run() {
                System.out.println("Jeee, kurssit haettu!");
                
                try {
                    for (Course get : courses.get()) {
                        System.out.println(get.getName());
                    }
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (ExecutionException ex) {
                    Exceptions.printStackTrace(ex);
                }
            
            }

        }, Executors.newFixedThreadPool(1));

        if(true) {
            return;
        }*/
        
        /*Futures.addCallback(courses, new FutureCallback<List<Course>>(){
            @Override
            public void onSuccess(List<Course> v) {
                for(Course c: v){
                    System.out.println(c.getName());
                }
            }

            @Override
            public void onFailure(Throwable thrwbl) {
                
            }         
        });*/
        
        BgTask.start("Refreshing course list", courseListTask, new BgTaskListener<List<Course>>() {

            @Override
            public void bgTaskReady(final List<Course> courses) {
                Course currentCourseStub = CourseListUtils.getCourseByName(courses, courseDb.getCurrentCourseName());
                if (currentCourseStub != null) {
                    CancellableCallable<Course> currentCourseTask = serverAccess.getFullCourseInfoTask(currentCourseStub);

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
