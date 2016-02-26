package fi.helsinki.cs.tmc.spyware;

import fi.helsinki.cs.tmc.utilities.JsonMaker;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;

/**
 * Generates host information used by spyware to identify requests coming from
 * single host.
 */
public class HostInformationGenerator {

    private static final Logger log = Logger.getLogger(HostInformationGenerator.class.getName());
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public String updateHostInformation(EventReceiver receiver) {
        JsonMaker data = getStaticHostInformation();
        // Should be unique enough not to collapse among singe users machines.


        String hostId = trySecureHash(data.toString());

        data.add("hostId", hostId);

        LoggableEvent event
                = new LoggableEvent(
                        "host_information_update",
                        data.toString().getBytes(Charset.forName("UTF-8")));
        receiver.receiveEvent(event);

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
                if (!networkInterface.isLoopback()) {
                    macs.add(trySecureHash(networkInterface.getHardwareAddress()));
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

    /**
     * Attempt to provide a reasonably ok hash of mac address. Should the
     * algorithm be missing original string is returned.
     */
    private static String trySecureHash(String mac) {
        return trySecureHash(mac.getBytes(UTF8));
    }

    private static String trySecureHash(byte[] mac) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(mac);
            return byteToHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            log.log(Level.WARNING, "Missing sha256 hash: {0}", ex);
            return byteToHex(mac);
        }
    }

    private static String byteToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }

}
