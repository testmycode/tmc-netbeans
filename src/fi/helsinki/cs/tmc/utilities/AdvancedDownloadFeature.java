package fi.helsinki.cs.tmc.utilities;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.JButton;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.data.ExerciseCollection;
import fi.helsinki.cs.tmc.ui.swingPanels.AdvancedExerciseDownloadPanel;

/**
 * For the future: Currently the advanced download doesn't allow for the
 * exercises to be sent, it only allows download. This might not be a problem
 * since the exercises are either expired and you shouldn't be able to send
 * them normally or they are still valid and you can just download them normally.
 * @author knordman
 */
public class AdvancedDownloadFeature {

    /**
     * Reads the selected exercises from the list and sends them to the Controller for downloading.
     * @param panel 
     */
    private static void download(AdvancedExerciseDownloadPanel panel) {
        ExerciseCollection collection = panel.getSelected();
        if (collection == null) {
            return;
        }

        fi.helsinki.cs.tmc.controller.Controller.getInstance().advancedDownload(collection);
    }

    /**
     * Check if a course list exists or not.
     * @return true If the list exists and false if not.
     * @throws IOException If CourseAndExerciseInfo throws one
     * @throws JSONException If CourseAndExerciseInfo throws one
     */
    private static boolean courseListExists() throws IOException {
        CourseCollection courses = null;
        courses = CourseAndExerciseInfo.getCourses();

        if (courses == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Show the advanced download dialog unless we can't find the course list.
     * @throws IOException
     */
    public static void show() throws IOException {
        try {
            if (!courseListExists()) {
                ModalDialogDisplayer.getDefault().displayNotification("Could not load courses from file. \n"
                        + "Go to preferences, fill all fields, refresh and try again.", "No courses present");
                return;
            }
        } catch (Exception e) {
            ModalDialogDisplayer.getDefault().displayError("Failed to fetch or read course list. Returner error:\n"
                    + e.getMessage() + "\n"
                    + "Check that all the settings in \"preferences\", refresh and try again");
            return;
        }

        final AdvancedExerciseDownloadPanel panel = new AdvancedExerciseDownloadPanel();

        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() instanceof JButton) {  //In the future someone might want to use some other method to invoke this.
                    ((JButton) e.getSource()).setEnabled(false);
                }
                download(panel);
            }
        });

        Object[] buttons = {downloadButton, DialogDescriptor.CANCEL_OPTION};

        DialogDescriptor descriptor = new DialogDescriptor(panel, "Advanced download", true, null);
        descriptor.setOptions(buttons);
        descriptor.setClosingOptions(buttons);

        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }
}
