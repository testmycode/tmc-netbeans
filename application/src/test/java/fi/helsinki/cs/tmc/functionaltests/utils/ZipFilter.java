package fi.helsinki.cs.tmc.functionaltests.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public abstract class ZipFilter {

    public byte[] filter(byte[] data) throws IOException {
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);
        filter(zis, zos);
        zis.close();
        zos.close();
        return bos.toByteArray();
    }

    public void filter(ZipInputStream zis, ZipOutputStream zos) throws IOException {
        ZipEntry zent;
        while ((zent = zis.getNextEntry()) != null) {
            if (zent.isDirectory()) {
                zos.putNextEntry(zent);
            } else {
                byte[] data = filterFile(zent, MyIOUtils.toByteArray(zis));
                if (data != null) {
                    ZipEntry newZent = new ZipEntry(zent.getName());
                    newZent.setSize(data.length);
                    zos.putNextEntry(newZent);
                    zos.write(data);
                    zos.closeEntry();
                }
            }
            zis.closeEntry();
        }
    }

    /**
     * Returns new contents for a file (or null to discard the file).
     */
    protected abstract byte[] filterFile(ZipEntry zent, byte[] data) throws IOException;
}
