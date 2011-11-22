package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.tailoring.Tailoring;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class TmcSettingsTest {
    private PersistableSettings store;
    @Mock private Tailoring tailoring;
    
    private TmcSettings settings;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        store = PersistableSettings.forModule(this.getClass());
        settings = newSettings();
        
        when(tailoring.getDefaultServerUrl()).thenReturn("http://default.example.com");
    }
    
    @After
    public void tearDown() {
        store.cancel();
    }
    
    private TmcSettings newSettings() {
        return new TmcSettings(store, tailoring);
    }
    
    @Test
    public void itUsesBaseUrlFromTailoringIfNoneStored() {
        assertEquals("http://default.example.com", settings.getServerBaseUrl());
    }
    
    @Test
    public void itUsesProjectRootDirFromTailoringIfNoneStored() {
        assertEquals("http://default.example.com", settings.getServerBaseUrl());
    }
    
    @Test
    public void itStripsTrailingSlashesOffTheBaseUrl() {
        settings.setServerBaseUrl("http://example.com");
        assertEquals("http://example.com", settings.getServerBaseUrl());
        
        settings.setServerBaseUrl("http://example.com/");
        assertEquals("http://example.com", settings.getServerBaseUrl());
        
        settings.setServerBaseUrl("http://example.com///////");
        assertEquals("http://example.com", settings.getServerBaseUrl());
        
        settings.setServerBaseUrl("http://example.com///////");
        assertEquals("http://example.com", newSettings().getServerBaseUrl());
    }
    
    @Test
    public void itDoesntStoreThePasswordByDefault() {
        assertFalse(settings.isSavingPassword());
        settings.setPassword("xoo");
        assertEquals("", newSettings().getPassword());
    }
    
    @Test
    public void itMayOptionallyStoreThePassword() {
        settings.setSavingPassword(true);
        settings.setPassword("xoo");
        
        assertTrue(settings.isSavingPassword());
        assertEquals("xoo", newSettings().getPassword());
        assertTrue(newSettings().isSavingPassword());
    }
    
    @Test
    public void itMayStoreThePasswordEvenAfterItHasBeenSet() {
        settings.setPassword("xoo");
        settings.setSavingPassword(true);
        
        assertTrue(settings.isSavingPassword());
        assertEquals("xoo", newSettings().getPassword());
        assertTrue(newSettings().isSavingPassword());
    }
    
    @Test
    public void itCanBeToldToRemoveAStoredPassword() {
        settings.setSavingPassword(true);
        settings.setPassword("xoo");
        settings.setSavingPassword(false);
        
        assertFalse(settings.isSavingPassword());
        assertEquals("xoo", settings.getPassword());
        
        assertEquals("", newSettings().getPassword());
        assertFalse(newSettings().isSavingPassword());
    }
}
