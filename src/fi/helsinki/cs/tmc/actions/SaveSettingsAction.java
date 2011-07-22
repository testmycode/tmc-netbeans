package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.ui.PreferencesPanel;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

public class SaveSettingsAction extends AbstractAction {
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!(e.getSource() instanceof PreferencesPanel)) {
            throw new IllegalArgumentException(
                    SaveSettingsAction.class.getSimpleName() +
                    " expected event source to be a " +
                    PreferencesPanel.class.getSimpleName()
                    );
        }
        
        PreferencesPanel prefPanel = (PreferencesPanel)e.getSource();
    }
    
}
