package fi.helsinki.cs.tmc.actions;

import java.awt.event.ActionEvent;
import fi.helsinki.cs.tmc.ui.PreferencesPanel;
import java.awt.Dialog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ShowSettingsActionTest {
    
    @Mock private DialogDisplayer displayer;
    @Mock private Dialog dialog;
    @Mock private SaveSettingsAction saveAction;
    @Captor private ArgumentCaptor<DialogDescriptor> descriptorCaptor;
    @Captor private ArgumentCaptor<ActionEvent> eventCaptor;
    private ShowSettingsAction action;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(displayer.createDialog(descriptorCaptor.capture())).thenReturn(dialog);
        action = new ShowSettingsAction(displayer, saveAction);
    }
    
    @Test
    public void itShowsThePreferencesDialog() {
        action.actionPerformed(null);
        verify(dialog).setVisible(true);
    }
    
    @Test
    public void itCallsTheSaveSettingsActionIfTheUserPressesOK() {
        action.actionPerformed(null);
        descriptorCaptor.getValue().getButtonListener().actionPerformed(
                new ActionEvent(DialogDescriptor.OK_OPTION, 0, null)
                );
        
        verify(saveAction).actionPerformed(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue());
        assertTrue(eventCaptor.getValue().getSource() instanceof PreferencesPanel);
    }
    
    @Test
    public void itDoesntCallTheSaveSettingsActionIfTheUserPressesOK() {
        action.actionPerformed(null);
        descriptorCaptor.getValue().getButtonListener().actionPerformed(
                new ActionEvent(DialogDescriptor.CANCEL_OPTION, 0, null)
                );
        
        verifyZeroInteractions(saveAction);
    }
}
