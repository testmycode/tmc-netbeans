package fi.helsinki.cs.tmc.data;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A CourseCollection is a collection of Course objects.
 * @author kkaltiai
 */
public class CourseCollection implements Iterable<Course> {
    private ArrayList<Course> items;

    public CourseCollection() {
    }

    /**
     * Searches if it can found an Course object by given coursename
     * @param courseName
     * @return null if it cannot find Course by given courseName or if 
     * given courseName parameter is null. Otherwise it returns a reference to course
     */
    public Course searchCourse(String courseName) { //TODO: rename to findCourse
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
    @Deprecated
    protected void processItem(Course item) {
        if (item == null) {
            throw new NullPointerException("course was null at CourseCollection.processItem");
        }

        if (searchCourse(item.getName()) != null) {
            throw new IllegalArgumentException("duplicate course: " + item.getName());
        }
    }

    /**
     * Adds an object to collection.
     * @param item
     */
    public void add(Course item) {
        if (item == null) {
            throw new NullPointerException("item was null at CollectionBase.add");
        }
        processItem(item);
        items.add(item);
    }

    /**
     * Removes an item from the collection.
     * @param item
     */
    @Deprecated
    public void remove(Course item) {
        if (item == null) {
            throw new NullPointerException("item was null at CollectionBase.remove");
        }
        items.remove(item);
    }

    /**
     * Return the amount of objects in this collection
     * @return
     */
    @Deprecated
    public int size() {
        return items.size();
    }

    /**
     * Return an item with the given index.
     * @param index
     * @return
     */
    @Deprecated
    public Course getItem(int index) {
        if (index < 0 || index >= items.size()) {
            throw new IndexOutOfBoundsException("index was invalid at CollectionBase.getItem");
        }
        return items.get(index);
    }

    /**
     * Return an iterator for this collection.
     * @return iterator to collection;
     */
    @Override
    public Iterator<Course> iterator() {
        return items.iterator();
    }
}