package fi.helsinki.cs.tmc.controller;

import javax.swing.JButton;
import fi.helsinki.cs.tmc.data.ExerciseCollection;

/**
 * An interface that the plugin's UI uses to invoke actions.
 */
public interface IController {
    void sendExerciseForReview(JButton source);

    void runTests();

    void showPreferences();

    void startExerciseOpening();

    void showAdvancedDownload();

    void advancedDownload(ExerciseCollection collection);
}
