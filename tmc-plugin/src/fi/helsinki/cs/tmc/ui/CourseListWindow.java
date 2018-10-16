package fi.helsinki.cs.tmc.ui;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.actions.ShowSettingsAction;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.model.CourseDb;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

public class CourseListWindow extends JPanel {

    private static JFrame frame;
    private final JLabel title;
    private final JList<Course> courses;
    private PreferencesPanel prefPanel;
    private static JButton button;

    public CourseListWindow(List<Course> courses, PreferencesPanel prefPanel) {
        this.prefPanel = prefPanel;
        this.title = new JLabel("Select a course:");
        Font titleFont = this.title.getFont();
        this.title.setFont(new Font(titleFont.getName(), Font.BOLD, 20));
        this.title.setBorder(new MatteBorder(new Insets(10, 10, 5, 10), getBackground()));
        Course[] courseArray = courses.toArray(new Course[courses.size()]);
        this.courses = new JList<>(courseArray);
        this.courses.setFixedCellHeight(107);
        this.courses.setFixedCellWidth(346);
        this.courses.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.button = new JButton("Select");
        button.addActionListener(new SelectCourseListener(this));

        this.courses.setCellRenderer(new CourseCellRenderer());
        this.courses.setVisibleRowCount(4);
        JScrollPane pane = new JScrollPane(this.courses);
        Dimension d = pane.getPreferredSize();
        d.width = 800;
        d.height = (int) (d.height * 1.12);
        pane.setPreferredSize(d);
        pane.setBorder(new EmptyBorder(5, 0, 5, 0));
        pane.setViewportBorder(new EmptyBorder(0, 0, 0, 0));
        pane.getVerticalScrollBar().setUnitIncrement(10);
        this.courses.setBackground(new Color(242, 241, 240));
        
        this.courses.setSelectedIndex(setDefaultSelectedIndex());
        this.courses.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() >= 2) {
                    button.doClick();
                }
            }
        });
        
        add(title);
        add(pane);
        add(button);
    }

    public static void display() throws Exception {
        PreferencesPanel prefPanel;
                if (PreferencesUIFactory.getInstance().getCurrentUI() == null) {
                    prefPanel = (PreferencesPanel) PreferencesUIFactory.getInstance().createCurrentPreferencesUI();
                } else {
                    prefPanel = (PreferencesPanel) PreferencesUIFactory.getInstance().getCurrentUI();
                }
        if (frame == null) {
            frame = new JFrame("Courses");
        }
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        List<Course> courses = prefPanel.getAvailableCourses();
        CourseDb.getInstance().setAvailableCourses(courses);
        final CourseListWindow courseListWindow = new CourseListWindow(courses, prefPanel);
        frame.setContentPane(courseListWindow);

        button.setMinimumSize(new Dimension(courseListWindow.getWidth(), button.getHeight()));
        button.setMaximumSize(new Dimension(courseListWindow.getWidth(), button.getHeight()));
        courseListWindow.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent event) {
                button.setMinimumSize(new Dimension(courseListWindow.getWidth(), button.getHeight()));
                button.setMaximumSize(new Dimension(courseListWindow.getWidth(), button.getHeight()));
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });

        if (hasCourses(courses, prefPanel)) {
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
    }
    
    public static boolean isWindowVisible() {
        if (frame == null) {
            return false;
        }
        return frame.isVisible();
    }
    
    private static boolean hasCourses(List<Course> courses, PreferencesPanel panel) {
        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "Organization has no courses!", "Error", JOptionPane.ERROR_MESSAGE);
            frame.setVisible(false);
            frame.dispose();
            return false;
        }
        return true;
    }
    
    private int setDefaultSelectedIndex() {
        final Optional<Course> currentCourse = TmcSettingsHolder.get().getCurrentCourse();
        if (!currentCourse.isPresent()) {
            return 0;
        }
        String selectedCourseName = currentCourse.get().getName();
        
        final ListModel<Course> list = courses.getModel();
        for (int i = 0; i < list.getSize(); i++) {
            if (list.getElementAt(i).getName().equals(selectedCourseName)) {
                return i;
            }
        }
        
        return 0;
    }

    class SelectCourseListener implements ActionListener {
                
        public SelectCourseListener(CourseListWindow window) {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            prefPanel.setSelectedCourse(courses.getSelectedValue());
            frame.setVisible(false);
            frame.dispose();
            new ShowSettingsAction().run();
        }
    }
}

class CourseCellRenderer extends DefaultListCellRenderer {
    
    private static final Color HIGHLIGHT_COLOR = new Color(240, 119, 70);
    
    private final Map<Course, CourseCard> cachedCourses;

    public CourseCellRenderer() {
        this.cachedCourses = new HashMap<>();
    }

    @Override
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean hasFocus) {
        final Course course = (Course)value;
        if (!this.cachedCourses.containsKey(course)) {
            this.cachedCourses.put(course, new CourseCard(course));
        }
        CourseCard courseCard = this.cachedCourses.get(course);
        if (isSelected) {
            courseCard.setColors(Color.white, HIGHLIGHT_COLOR);
        } else {
            courseCard.setColors(new Color(76, 76, 76), Color.white);
        }
        return courseCard;
    }
}
