package fi.helsinki.cs.tmc.spyware;

import fi.helsinki.cs.tmc.events.TmcEventBus;
import fi.helsinki.cs.tmc.utilities.JsonMaker;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates host information used by spyware to identify requests coming from single host.
 *
 * HostInformation is linked by hopefully unique enough small identifier which is calculated from
 * host information.
 */
public class HostInformationGenerator {

    private static final Logger log = Logger.getLogger(HostInformationGenerator.class.getName());

    public int updateHostInformation() {
        JsonMaker data = getStaticHostInformation();
        // Should be unique enough not to collapse among singe users machines.
        int hostId = data.toString().hashCode();

        data.add("hostId", hostId);

        LoggableEvent event =
                new LoggableEvent(
                        "host_information_update",
                        data.toString().getBytes(Charset.forName("UTF-8")));
        TmcEventBus.getDefault().post(event);

        return hostId;
    }

    private static JsonMaker getStaticHostInformation() {
        JsonMaker builder = JsonMaker.create();

        try {
            java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
            builder.add("hostAddress", localMachine.getHostAddress());
            builder.add("hostName", localMachine.getHostName());
        } catch (Exception ex) {
            log.log(Level.WARNING, "Exception while getting host name information: {0}", ex);
        }

        try {
            Enumeration<NetworkInterface> iterator = NetworkInterface.getNetworkInterfaces();
            List<String> macs = new ArrayList<String>(2);
            while (iterator.hasMoreElements()) {
                NetworkInterface networkInterface = iterator.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    byte[] mac = networkInterface.getHardwareAddress();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                    }
                    macs.add(sb.toString());
                }
            }
            builder.add("mac_addresses", macs);

        } catch (Exception ex) {
            log.log(Level.WARNING, "Exception while getting host mac information: {0}", ex);
        }

        try {
            builder.add("user.name", System.getProperty("user.name"));
            builder.add("java.runtime.version", System.getProperty("java.runtime.version"));
            builder.add("os.name", System.getProperty("os.name"));
            builder.add("os.version", System.getProperty("os.version"));
        } catch (Exception e) {
            log.log(Level.WARNING, "Exception while getting basic host information: {0}", e);
        }

        return builder;
    }
}
