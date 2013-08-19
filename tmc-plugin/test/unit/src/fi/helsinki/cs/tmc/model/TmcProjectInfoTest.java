package fi.helsinki.cs.tmc.model;

import org.openide.filesystems.FileObject;
import org.netbeans.api.project.Project;
import fi.helsinki.cs.tmc.testing.TempTestDir;
import fi.helsinki.cs.tmc.utilities.zip.RecursiveZipper;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openide.filesystems.FileUtil;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class TmcProjectInfoTest {
    private static final String SLASH = File.separator;
    private TempTestDir tempDir;
    private String mainDir;
    private Project mockProject;
    private TmcProjectInfo projectInfo;
    
    @Before
    public void setUp() throws IOException {
        tempDir = new TempTestDir();
        
        mainDir = tempDir.getPath() + SLASH + "MyExercise";
        new File(mainDir + SLASH + "src" + SLASH + "subdir").mkdirs();
        new File(mainDir + SLASH + "src" + SLASH + "Included1.txt").createNewFile();
        new File(mainDir + SLASH + "src" + SLASH + "subdir" + SLASH + "Included2.txt").createNewFile();
        
        new File(mainDir + SLASH + "Excluded.txt").createNewFile();
        new File(mainDir + SLASH + "test").mkdir();
        new File(mainDir + SLASH + "test" + SLASH + "Excluded.txt").createNewFile();
        new File(mainDir + SLASH + "test").mkdir();
        new File(mainDir + SLASH + "excluded").mkdir();
        new File(mainDir + SLASH + "excluded" + SLASH + "Foo.txt").createNewFile();
        
        mockProject = mock(Project.class);
        
        FileObject mainDirFo = FileUtil.toFileObject(new File(mainDir));
        assertNotNull("masterfs module likely not enabled for tests", mainDirFo);
        
        when(mockProject.getProjectDirectory()).thenReturn(mainDirFo);
        projectInfo = new TmcProjectInfo(mockProject);
    }
    
    @After
    public void tearDown() throws IOException {
        tempDir.destroy();
    }
    
    @Test
    public void itShouldChooseToZipSourceFilesInRegularJavaProjects() throws IOException {
        new File(mainDir + SLASH + "src").mkdir();
        new File(mainDir + SLASH + "src" + SLASH + "Foo.txt").createNewFile();
        
        RecursiveZipper.ZippingDecider zd = projectInfo.getZippingDecider();
        assertTrue(zd.shouldZip("MyExercise/src/FooTest.txt"));
    }
    
    @Test
    public void itShouldNotChooseToZipTestFilesInRegularJavaProjectsByDefault() throws IOException {
        new File(mainDir + SLASH + "test").mkdir();
        new File(mainDir + SLASH + "test" + SLASH + "FooTest.txt").createNewFile();
        
        RecursiveZipper.ZippingDecider zd = projectInfo.getZippingDecider();
        assertFalse(zd.shouldZip("MyExercise/test/FooTest.txt"));
    }
    
    @Test
    public void itShouldChooseToZipTestFilesThatAreSpecifiedInTheProjectFile() throws IOException {
        new File(mainDir + SLASH + "test").mkdir();
        new File(mainDir + SLASH + "test" + SLASH + "IncludedTest.txt").createNewFile();
        
        FileUtils.write(new File(mainDir + SLASH + ".tmcproject.yml"), "extra_student_files:\n  - test/IncludedTest.txt", "UTF-8");
        
        RecursiveZipper.ZippingDecider zd = projectInfo.getZippingDecider();
        assertTrue(zd.shouldZip("MyExercise/test/IncludedTest.txt"));
        assertFalse(zd.shouldZip("MyExercise/test/Excluded.txt"));
    }
    
    @Test
    public void itShouldChooseToZipEverythingInMavenProjects() throws IOException {
        new File(mainDir + SLASH + "src" + SLASH + "main" + SLASH + "java").mkdirs();
        new File(mainDir + SLASH + "src" + SLASH + "test" + SLASH + "java").mkdirs();
        new File(mainDir + SLASH + "xoox").mkdirs();
        new File(mainDir + SLASH + "src" + SLASH + "main" + SLASH + "java" + "Foo.txt").createNewFile();
        new File(mainDir + SLASH + "src" + SLASH + "test" + SLASH + "java" + "Bar.txt").createNewFile();
        new File(mainDir + SLASH + "Baz.txt").createNewFile();
        new File(mainDir + SLASH + "xoox" + "Zab.txt").createNewFile();
        new File(mainDir + SLASH + "pom.xml").createNewFile();
        
        RecursiveZipper.ZippingDecider zd = projectInfo.getZippingDecider();
        assertTrue(zd.shouldZip("MyExercise/src/main/java/Foo.txt"));
        assertTrue(zd.shouldZip("MyExercise/src/test/java/Bar.txt"));
        assertTrue(zd.shouldZip("MyExercise/Baz.txt"));
        assertTrue(zd.shouldZip("MyExercise/xoox/Zab.txt"));
        assertTrue(zd.shouldZip("MyExercise/pom.xml"));
    }
}
