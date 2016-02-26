package fi.helsinki.cs.tmc.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public class ConfigFile {

    private final String name;
    private FileObject fileObject;

    public ConfigFile(String name) {
        this.name = name;
    }

    public FileObject getFileObject() throws IOException {
        if (fileObject == null) {
            FileObject root = FileUtil.getConfigRoot();
            FileObject tmcRoot = root.getFileObject("tmc");
            if (tmcRoot == null) {
                tmcRoot = root.createFolder("tmc");
            }
            this.fileObject = tmcRoot.getFileObject(name);
            if (this.fileObject == null) {
                this.fileObject = tmcRoot.createData(name);
            }
        }
        return fileObject;
    }

    public boolean exists() throws IOException {
        return getFileObject().getSize() > 0;
    }

    public Writer getWriter() throws IOException {
        return new OutputStreamWriter(new BufferedOutputStream(getFileObject().getOutputStream()), "UTF-8");
    }

    public Reader getReader() throws IOException {
        return new InputStreamReader(new BufferedInputStream(getFileObject().getInputStream()), "UTF-8");
    }

    public void writeContents(String s) throws IOException {
        Writer w = getWriter();
        try {
            w.write(s);
        } finally {
            w.close();
        }
    }

    public String readContents() throws IOException {
        return getFileObject().asText("UTF-8");
    }
}
