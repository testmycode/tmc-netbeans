package fi.helsinki.cs.tmc.ui;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.actions.ShowSettingsAction;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.model.CourseDb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

public class CourseListWindow extends JPanel {

    private static JFrame frame;
    private final JList<CourseCard> courses;
    private PreferencesPanel prefPanel;

    public CourseListWindow(List<Course> courses, PreferencesPanel prefPanel) {
        this.prefPanel = prefPanel;       
        CourseCard[] courseCards = new CourseCard[courses.size()];
        for (int i = 0; i < courses.size(); i++) {
            courseCards[i] = new CourseCard(courses.get(i));
        }
        this.courses = new JList<>(courseCards);
        this.courses.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayout(new BorderLayout());
        JButton button = new JButton("Select");
        button.addActionListener(new SelectCourseListener(this));

        this.courses.setCellRenderer(new CourseCellRenderer());
        this.courses.setVisibleRowCount(4);
        JScrollPane pane = new JScrollPane(this.courses);
        Dimension d = pane.getPreferredSize();
        d.width = 400;
        pane.setPreferredSize(d);
        pane.setBorder(new EmptyBorder(5,0,5,0));
        this.courses.setBackground(new Color(242, 241, 240));
        
        this.courses.setSelectedIndex(setDefaultSelectedIndex());

        add(pane, BorderLayout.NORTH);
        add(button, BorderLayout.SOUTH);
    }

    public static void display(PreferencesPanel prefPanel) throws Exception {
        if (frame == null) {
            frame = new JFrame("Courses");
        }
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        List<Course> courses = prefPanel.getAvailableCourses();
        CourseDb.getInstance().setAvailableCourses(courses);
        frame.setContentPane(new CourseListWindow(courses, prefPanel));
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
        
        final ListModel<CourseCard> list = courses.getModel();
        for (int i = 0; i < list.getSize(); i++) {
            if (list.getElementAt(i).getCourse().getName().equals(selectedCourseName)) {
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
            prefPanel.setSelectedCourse(courses.getSelectedValue().getCourse());
            frame.setVisible(false);
            frame.dispose();
            new ShowSettingsAction().run();
        }
    }
}

class CourseCellRenderer extends JLabel implements ListCellRenderer {
    
    private static final Color HIGHLIGHT_COLOR = new Color(240, 119, 70);
    
    public CourseCellRenderer() {
    }

    @Override
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean hasFocus) {
        CourseCard course = (CourseCard) value;
        if (isSelected) {
            course.setColors(Color.white, HIGHLIGHT_COLOR);
        } else {
            course.setColors(new Color(76, 76, 76), Color.white);
        }
        return course;
    }
}
