package fi.helsinki.cs.tmc.model;

import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.tailoring.Tailoring;
import java.util.Locale;
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
    private TmcEventBus eventBus;
    
    private NBTmcSettings settings;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        store = PersistableSettings.forModule(this.getClass());
        settings = newSettings();
        eventBus = TmcEventBus.createNewInstance();
        
        when(tailoring.getDefaultServerUrl()).thenReturn("http://default.example.com");
    }
    
    @After
    public void tearDown() {
        store.cancel();
    }
    
    private NBTmcSettings newSettings() {
        return new NBTmcSettings(store, tailoring, eventBus);
    }
    
    @Test
    public void itUsesBaseUrlFromTailoringIfNoneStored() {
        assertEquals("http://default.example.com", settings.getServerAddress());
    }
    
    @Test
    public void itUsesProjectRootDirFromTailoringIfNoneStored() {
        assertEquals("http://default.example.com", settings.getServerAddress());
    }
    
    @Test
    public void itStripsTrailingSlashesOffTheBaseUrl() {
        settings.setServerBaseUrl("http://example.com");
        assertEquals("http://example.com", settings.getServerAddress());
        
        settings.setServerBaseUrl("http://example.com/");
        assertEquals("http://example.com", settings.getServerAddress());
        
        settings.setServerBaseUrl("http://example.com///////");
        assertEquals("http://example.com", settings.getServerAddress());
        
        settings.setServerBaseUrl("http://example.com///////");
        assertEquals("http://example.com", settings.getServerAddress());
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
    
    @Test
    public void itSavesWhetherToCheckUpdatesInTheBackground() {
        assertTrue(settings.isCheckingForUpdatesInTheBackground());
        settings.setCheckingForUpdatesInTheBackground(false);
        assertFalse(newSettings().isCheckingForUpdatesInTheBackground());
        settings.setCheckingForUpdatesInTheBackground(true);
        assertTrue(newSettings().isCheckingForUpdatesInTheBackground());
    }
    
    @Test
    public void localeToStringBehavesAsExpected() {
        assertEquals("fi", new Locale("fi").toString());
        assertEquals("fi_FI", new Locale("fi", "FI").toString());
        assertEquals("fi_FI_foo", new Locale("fi", "FI", "foo").toString());
    }
}
