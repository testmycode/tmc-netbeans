package fi.helsinki.cs.tmc.server;

import fi.helsinki.cs.tmc.Refactored;
import fi.helsinki.cs.tmc.data.CourseCollection;
import fi.helsinki.cs.tmc.utilities.http.FileDownloader;
import fi.helsinki.cs.tmc.utilities.json.parsers.JSONCourseListParser;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * A frontend for the server.
 */
@Refactored
public class TmcServerAccess {

    private String baseUrl;
    private FileDownloader fileDownloader;
    
    public TmcServerAccess(String baseUrl, FileDownloader fileDownloader) {
        this.baseUrl = baseUrl;
        this.fileDownloader = fileDownloader;
    }
    
    public Future<CourseCollection> startDownloadingCourseList(BgTaskListener<CourseCollection> listener) {
        Callable<CourseCollection> task = new Callable<CourseCollection>() {
            @Override
            public CourseCollection call() throws Exception {
                String json = fileDownloader.downloadTextFile(getCourseListUrl());
                return JSONCourseListParser.parseJson(json);
            }
        };
        return BgTask.start("Download " + getCourseListUrl(), listener, task);
    }
    
    private String getCourseListUrl() {
        return baseUrl + "/courses.json";
    }
    
}
