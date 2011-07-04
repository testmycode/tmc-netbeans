package palikka.controller;

import javax.swing.JButton;
import palikka.data.ExerciseCollection;

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
