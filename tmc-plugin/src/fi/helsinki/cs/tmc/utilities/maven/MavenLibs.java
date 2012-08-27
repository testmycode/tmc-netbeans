package fi.helsinki.cs.tmc.utilities.maven;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;

public class MavenLibs {
    private static ClassPath mavenClassPath;
    
    public static ClassPath getMavenClassPath() {
        if (mavenClassPath == null) {
            File mavenJarsBase = InstalledFileLocator.getDefault().locate("modules/ext", "fi.helsinki.cs.tmc.maven.wrapper", false);

            Collection<File> files = FileUtils.listFiles(mavenJarsBase, new String[] {"jar"}, true);
            ArrayList<URL> urls = new ArrayList<URL>();
            for (File file : files) {
                URL url = FileUtil.urlForArchiveOrDir(file);
System.out.println(">>>>>>>>>>> " + url);
                if (url != null) {
                    urls.add(url);
                }
            }
            mavenClassPath = ClassPathSupport.createClassPath(urls.toArray(new URL[urls.size()]));
System.out.println("============== " + mavenClassPath.toString(ClassPath.PathConversionMode.WARN));
        }
        return mavenClassPath;
    }
}
