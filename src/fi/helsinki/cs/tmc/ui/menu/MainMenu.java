package fi.helsinki.cs.tmc.ui.menu;

import java.awt.event.ActionEvent;
import fi.helsinki.cs.tmc.controller.Controller;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import fi.helsinki.cs.tmc.controller.IController;

/**
 * Palikka uses this class to create a menu for NetBeans to show.
 * @author jmturpei
 */
public class MainMenu extends JMenu {

    /*
     * A list of items that appear in the menu.
     */
    private JMenuItem preferences;
    private JMenuItem exercises;
    private JMenuItem experimental;  //Should be "advanced download" but since it was released towards the end
                                     //I will leave it this way
    private IController controller;

    /**
     * Constructor
     */
    public MainMenu() {
        controller = Controller.getInstance();

        this.setText("Palikka");
        preferences = new JMenuItem("Preferences");
        exercises = new JMenuItem("Show exercises");
        exercises.setToolTipText("Downloads new exercises and opens still valid exercises.");
        experimental = new JMenuItem("Advanced download");

        this.add(preferences);
        this.add(new JSeparator());
        this.add(exercises);
        this.add(new JSeparator());
        this.add(experimental);

        preferences.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controller.showPreferences();
            }
        });

        exercises.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controller.startExerciseOpening();
            }
        });

        experimental.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                controller.showAdvancedDownload();
            }
        });
    }
}
