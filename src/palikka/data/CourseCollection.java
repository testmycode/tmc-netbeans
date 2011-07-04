package palikka.data;

/**
 * A CourseCollection is a collection of Course objects.
 * @author kkaltiai
 */
public class CourseCollection extends CollectionBase<Course> {

    public CourseCollection() {
    }

    /**
     * Searches if it can found an Course object by given coursename
     * @param courseName
     * @return null if it cannot find Course by given courseName or if 
     * given courseName parameter is null. Otherwise it returns a reference to course
     */
    public Course searchCourse(String courseName) {
        if (courseName == null) {
            return null;
        }

        for (Course course : this) {
            if (course.getName().equals(courseName)) {
                return course;
            }
        }

        return null;
    }

    /**
     * Method is called whenever a new object is added to this collection.
     * The method checks that the item is not null and that there are no duplicates.
     * @param item 
     */
    @Override
    protected void processItem(Course item) {
        if (item == null) {
            throw new NullPointerException("course was null at CourseCollection.processItem");
        }

        if (searchCourse(item.getName()) != null) {
            throw new IllegalArgumentException("duplicate course: " + item.getName());
        }
    }
}