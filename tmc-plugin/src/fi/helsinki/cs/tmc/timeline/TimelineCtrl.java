package fi.helsinki.cs.tmc.timeline;

import fi.helsinki.cs.tmc.model.CourseDb;

import net.java.html.js.JavaScriptBody;
import net.java.html.js.JavaScriptResource;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.Property;
import org.netbeans.api.htmlui.OpenHTMLRegistration;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle;

@Model(className = "Timeline", targetId = "", properties = {
    @Property(name = "text", type = String.class)
})
public final class TimelineCtrl {

    @ComputedProperty
    static String templateName() {
        return "window";
    }

    @ComputedProperty
    public static String getCurrentWeekName() {
        return "week" + getWeek();
    }

    private static int getWeek() {
        return CourseDb.getInstance().getPresumedCurrentWeek();
    }

    @ActionID(
        category = "TMC",
        id = "timeline.Timeline"
    )
    @ActionReference(path = "Menu/TM&C", position = -350, separatorAfter = -300)
    @NbBundle.Messages("CTL_Timeline=Timeline")
    @OpenHTMLRegistration(
        url = "Timeline.html",
        displayName = "#CTL_Timeline")
    public static Timeline onPageLoad() {
        TimelineJson json = new TimelineJson(getWeek());
        TimelineWidget.drawTimeline(json.getExerciseColorJsonMap(), json.getSkillColorJsonMap());
        return new Timeline("Timeline").applyBindings();
    }

    @JavaScriptResource(value = "Timeline.js")
    public static final class TimelineWidget {
        private TimelineWidget() {
        }

        @JavaScriptBody(args = { "exerciseMap", "skillMap" },
            body = "timelineWrapper.drawTimeline(exerciseMap, skillMap);")
        public static native void drawTimeline(String exerciseMap, String skillMap);
    }

}
