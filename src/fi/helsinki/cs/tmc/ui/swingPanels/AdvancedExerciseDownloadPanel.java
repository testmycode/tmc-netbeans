package fi.helsinki.cs.tmc.ui.swingPanels;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import fi.helsinki.cs.tmc.settings.PluginSettings;
import fi.helsinki.cs.tmc.utilities.LocalCourseCache;
import fi.helsinki.cs.tmc.utilities.http.FileDownloaderAsync;
import fi.helsinki.cs.tmc.utilities.http.IDownloadListener;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONExerciseListParser;
import fi.helsinki.cs.tmc.utilities.textio.StreamToString;

/**
 * This panel displays the advanced download dialog for all the exercises available
 * including expired ones.
 * @author knordman
 */
public class AdvancedExerciseDownloadPanel extends JPanel implements IDownloadListener {

    /**
     * This container holds checkboxes for each individual exercise
     */
    private Box exerciseBox;
    /**
     * This holds a list of all the courses available.
     */
    private JComboBox courseBox;
    /**
     * This wraps the exerciseBox and sometimes JLabels displaying errors and information
     * regarding the exercise lists.
     */
    private JScrollPane pane;
    public FileDownloaderAsync downloader;
    /**
     * This serves as a cache so we don't have to download the exercise lists more than once.
     */
    private HashMap<Course, ExerciseCollection> course2elist;
    /**
     * When this is true, local exercise lists and cached exercise lists are bypassed and
     * the exercise list is always updated from the server.
     */
    private boolean forceUpdate;

    public AdvancedExerciseDownloadPanel() throws IOException {
        this.course2elist = new HashMap<Course, ExerciseCollection>();
        this.forceUpdate = false;
        initComponents();
    }

    /**
     * Called only by the constructor to create the layout and the components
     */
    private void initComponents() throws IOException {
        setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(316, 300));

        //Initialize variables
        this.courseBox = new JComboBox();
        this.exerciseBox = Box.createVerticalBox();
        GridBagConstraints c = new GridBagConstraints();

        //Add the combobox
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;

        add(courseBox, c);

        //Add the Force update button (may the JButton be with you!)
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;

        final JButton forcedUpdate = new JButton("Force update");
        forcedUpdate.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                forcedUpdate.setEnabled(false);
                forceUpdate = true;
            }
        });

        add(forcedUpdate, c);

        // Add the box
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;

        pane = new JScrollPane(this.exerciseBox);
        pane.setMinimumSize(new Dimension(300, 250));
        pane.setPreferredSize(new Dimension(300, 250));

        add(pane, c);

        //Initialize courseBox and exercise list
        try {
            initCombo();
        } catch (Exception ex) {
            throw new IOException("Could not load courses from file. \n"
                    + "Go to preferences, fill all fields, refresh and try again.");
        }

        //initExerciseBox();

        courseBox.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateExerciseList((Course) courseBox.getSelectedItem());
            }
        });
    }

    /**
     * Initial initialization of the course JComboBox.
     * Throws exceptions if something goes wrong because without the course list
     * we can't really do anything.
     * 
     * For the future: It might be a good idea to implement some sort of
     * downloading mechanism for the course list or perhaps just add this
     * to the preferences panel and give it a cool new name.
     * @throws IOException
     * @throws JSONException 
     */
    private void initCombo() throws IOException {
        courseBox.removeAllItems();

        CourseCollection courses = LocalCourseCache.getInstance().getCourses();

        if (courses == null) {
            throw new IOException("Could not load courses from file. \n"
                    + "Go to preferences, fill all fields, refresh and try again.");
        }


        Course selectedCourse = courses.getCourseByName(PluginSettings.getSettings().getSelectedCourse());
        int i = -1;
        int selectedCourseIndex = -1;

        for (Course course : courses) {
            i++;
            courseBox.addItem(course);

            if (course.getName().equals(selectedCourse.getName())) {
                selectedCourseIndex = i;
            }
        }
        courseBox.setSelectedIndex(selectedCourseIndex);
    }

    /**
     * Display an error message in the exercise list. Both parameters can be null.
     * @param message Optional message or null
     * @param ex Optional Exception or null
     */
    private void displayExerciseFailure(String message, Exception ex) {
        exerciseBox.removeAll();
        if (message != null) {
            exerciseBox.add(new JLabel(message));
        }
        if (ex != null) {
            exerciseBox.add(new JLabel(ex.getMessage()));
        }

        exerciseBox.setVisible(true);

        pane.revalidate();
        pane.repaint();
        return;
    }

    /**
     * This method updates the exercise box and the scrollpane to show the exercises in the given ExerciseCollection
     * @param ec new list of exercises to show
     */
    private void updateExerciseBox(ExerciseCollection ec) {
        if (ec == null) {
            return;
        }

        course2elist.put(ec.getCourse(), ec);

        exerciseBox.removeAll();
        for (Exercise exe : ec) {
            this.exerciseBox.add(new JCheckBox(exe.getName()));
        }
        exerciseBox.setVisible(true);

        pane.revalidate();  //This (or validate) must be used with remove() and removeAll()!
        pane.repaint();

    }

    /**
     * Attempt to update the list from cache.
     * @return true if the update was successfull and false if not.
     */
    private boolean cacheUpdate(Course course) {
        ExerciseCollection ec = course2elist.get(course);
        if (ec != null) {
            updateExerciseBox(ec);
            return true;
        }

        return false;
    }

    /**
     * Iniate exercise list downloading. This does not write it to file just the cache.
     * @param course 
     */
    private void downloadExerciseList(Course course) {
        if (!forceUpdate) {
            if (cacheUpdate(course)) {
                return;
            }
        }
        exerciseBox.removeAll();
        exerciseBox.add(new JLabel("Downloading..."));

        exerciseBox.setVisible(true);

        pane.revalidate();
        pane.repaint();

        try {
            if (this.downloader != null) {
                this.downloader.cancel();
            }
            this.downloader = new FileDownloaderAsync(course.getExerciseListDownloadAddress(), this);
            this.downloader.download("Downloading exercise list");
        } catch (Exception ex) {
            displayExerciseFailure("Download failed! Returned error:", ex);
        }
    }

    /**
     * Attempts to update the exercise list from file and tries to update it from the server if it fails.
     * If forced update is enabled, tries to download.
     * @param course 
     */
    private void updateExerciseList(Course course) {
        if (forceUpdate) {
            downloadExerciseList(course);
        } else {
            ExerciseCollection ec = null;
            try {
                ec = LocalCourseCache.getInstance().getExercises(course);
            } catch (Exception ex) {
                downloadExerciseList(course);
                return;
            }

            if (ec != null) {
                updateExerciseBox(ec);
            } else {
                downloadExerciseList(course);
            }
        }
    }

    /**
     * The initial initialization of the ExerciseBox.
     * This is immune to errors as it is likely that there are no exercises anywhere to be found
     */
    private void initExerciseBox() {
        exerciseBox.removeAll();

        exerciseBox.setVisible(true);

        pane.revalidate();  //This (or validate) must be used with remove() and removeAll()!
        pane.repaint();
    }

    private ExerciseCollection getExercises(Course course) {
        ExerciseCollection ec = null;

        ec = course2elist.get(course);
        return ec;
    }

    /**
     * Is used to get a list of selected exercises.
     * @return ExerciseCollection consisting of the selected exercises or null if something went wrong.
     */
    public ExerciseCollection getSelected() {
        Course selectedCourse = (Course) courseBox.getSelectedItem();
        if (selectedCourse == null) {
            return null;
        }

        ExerciseCollection ec = new ExerciseCollection(selectedCourse);
        if (ec == null) {
            return null;
        }

        ExerciseCollection allExercises = course2elist.get(selectedCourse);
        if (allExercises == null) {
            return null;
        }

        Object[] components = exerciseBox.getComponents();

        ArrayList<String> exerciseNames = new ArrayList<String>();

        for (Object obj : components) {
            if (obj instanceof JCheckBox) {
                if (((JCheckBox) obj).isSelected()) {
                    exerciseNames.add(((JCheckBox) obj).getAccessibleContext().getAccessibleName());  //This is how the name is hidden
                }
            }
        }

        for (String str : exerciseNames) {
            Exercise next = allExercises.getExerciseByName(str);
            if (next != null) {
                ec.add(next);
            }
        }

        return ec;
    }

    /**
     * updates the exercise box from a string which should be in JSON form.
     * @Return false upon failure and true upon success
     */
    private void parseAndUpdateCache(String jsonString) {
        ExerciseCollection ec = null;
        try {
            ec = JSONExerciseListParser.parseJson(jsonString, (Course) courseBox.getSelectedItem());
        } catch (Exception ex) {
            displayExerciseFailure("Download succeeded but returned error while processing json:", ex);
        }

        if (ec == null) {
            displayExerciseFailure("Downloaded a list but the list was empty!", null);
        }

        //IF we get here we should write to cache and update the list

        this.course2elist.put((Course) courseBox.getSelectedItem(), ec);
        updateExerciseBox(ec);

    }

    /**
     * Called by the downloader when the download is completed.
     * @param source 
     */
    @Override
    public void downloadCompleted(FileDownloaderAsync source) {
        InputStream in = source.getFileContent();
        String json = null;
        try {
            json = StreamToString.inputStreamToString(in);
        } catch (Exception ex) {
            displayExerciseFailure("Download succeeded but returned error while processing downloaded data:", ex);
        }

        parseAndUpdateCache(json);
    }

    /**
     * Called by the downloader when the download fails
     * @param source 
     */
    @Override
    public void downloadFailed(FileDownloaderAsync source) {
        displayExerciseFailure("Download failed: \n" + source.getErrorMsg(), null);
    }

    @Override
    public void downloadCancelledByUser(FileDownloaderAsync source) {
        return;
    }
}
