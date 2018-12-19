package fi.helsinki.cs.tmc.utilities;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URI;

import org.openide.awt.HtmlBrowser;

public class BrowserOpener {

    public static void openUrl(URI url) throws URISyntaxException, IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(url);
        } else {
            HtmlBrowser.URLDisplayer.getDefault().showURLExternal(url.toURL());
        }
    }
}
