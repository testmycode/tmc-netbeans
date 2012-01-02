package fi.helsinki.cs.tmc.functionaltests.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

// Invoking FileUtils.readFileToByteArray in test code would cause
// WARNING [org.netbeans.ProxyClassLoader]: Will not load class org.apache.commons.io.FileUtils arbitrarily from one of ModuleCL@67afe460[org.netbeans.modules.extexecution.destroy] and ModuleCL@71d9d55b[TestMyCode] starting from SystemClassLoader[394 modules]; see http://wiki.netbeans.org/DevFaqModuleCCE
// grr :(
public class MyIOUtils {
    public static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        while (true) {
            int amt = is.read(buf);
            if (amt == -1) {
                break;
            }
            bos.write(buf, 0, amt);
        }
        return bos.toByteArray();
    }

}
