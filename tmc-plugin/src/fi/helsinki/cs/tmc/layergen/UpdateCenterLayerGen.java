package fi.helsinki.cs.tmc.layergen;

import fi.helsinki.cs.tmc.tailoring.SelectedTailoring;
import fi.helsinki.cs.tmc.tailoring.Tailoring;
import fi.helsinki.cs.tmc.utilities.urlcallback.CallbackURLStreamHandler;
import fi.helsinki.cs.tmc.utilities.urlcallback.URLCallback;

import org.openide.filesystems.Repository;
import org.openide.filesystems.Repository.LayerProvider;
import org.openide.util.lookup.ServiceProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * Generates an update center instance based on the selected tailoring.
 */
@ServiceProvider(service=LayerProvider.class)
public class UpdateCenterLayerGen extends Repository.LayerProvider {
    private static final String CALLBACK_NAME = "/" + UpdateCenterLayerGen.class.getCanonicalName();

    private static URLCallback callback = new URLCallback() {
        @Override
        public String getInputEncoding() {
            return "UTF-8";
        }

        @Override
        public InputStream openInputStream() {
            try {
                return new ByteArrayInputStream(getInstanceXML().getBytes("UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
        }
    };

    private static boolean callbackRegistered = false;

    public UpdateCenterLayerGen() {
        synchronized (UpdateCenterLayerGen.class) {
            if (!callbackRegistered) {
                CallbackURLStreamHandler.registerCallback(Paths.get(CALLBACK_NAME), callback);
            }
            callbackRegistered = true;
        }
    }

    @Override
    protected void registerLayers(Collection<? super URL> layers) {
        try {
            layers.add(new URL("callback", "", CALLBACK_NAME));
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String getInstanceXML() {
        Tailoring tailoring = SelectedTailoring.get();

        return
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE filesystem PUBLIC \"-//NetBeans//DTD Filesystem 1.2//EN\" \"http://www.netbeans.org/dtds/filesystem-1_2.dtd\">\n" +
                "<filesystem>" +
                "    <folder name=\"Services\">\n" +
                "        <folder name=\"AutoupdateType\">\n" +
                "            <file name=\"fi_helsinki_cs_tmc_update_center.instance\">\n" +
                "                <attr name=\"displayName\" stringvalue=\"" + tailoring.getUpdateCenterTitle() + "\"/>\n" +
                "                <attr name=\"enabled\" boolvalue=\"true\"/>\n" +
                "                <attr name=\"instanceCreate\" methodvalue=\"org.netbeans.modules.autoupdate.updateprovider.AutoupdateCatalogFactory.createUpdateProvider\"/>\n" +
                "                <attr name=\"instanceOf\" stringvalue=\"org.netbeans.spi.autoupdate.UpdateProvider\"/>\n" +
                "                <attr name=\"url\" stringvalue=\"" + tailoring.getUpdateCenterUrl() + "\"/>\n" +
                "           </file>\n" +
                "        </folder>\n" +
                "    </folder>\n" +
                "</filesystem>";
    }
}