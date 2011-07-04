package fi.helsinki.cs.tmc.controller;

import javax.swing.JButton;
import fi.helsinki.cs.tmc.data.ExerciseCollection;

/**
 * An interface that the plugin's panels use to invoke certain actions.
 * @author jmturpei
 */
public interface IController {

    void send(JButton source);

    void runTests();

    void showPreferences();

    void startExerciseOpening();

    void showAdvancedDownload();

    void advancedDownload(ExerciseCollection collection);
}
