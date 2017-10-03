package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.LocalExerciseStatus;
import fi.helsinki.cs.tmc.ui.OpenClosedExercisesDialog;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(category = "TMC",
        id = "fi.helsinki.cs.tmc.actions.OpenClosedExercisesFromMenu")
@ActionRegistration(displayName = "#CTL_OpenClosedExercisesFromMenu")
@ActionReferences({
    @ActionReference(path = "Menu/TM&C", position = -43)
})
@NbBundle.Messages("CTL_OpenClosedExercisesFromMenu=&Open closed exercises")
public class OpenClosedExercisesFromMenu extends AbstractAction {    
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Course course = CourseDb.getInstance().getCurrentCourse();
        final LocalExerciseStatus status = LocalExerciseStatus.get(course.getExercises());
        OpenClosedExercisesDialog.display(status.closed);
    }
}
