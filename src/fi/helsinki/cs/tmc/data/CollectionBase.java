package fi.helsinki.cs.tmc.data;

import java.util.*;

/**
 * Generic base class for iterable collections.
 * This class is inherited by CourseCollection and ExerciseCollection.
 * @author jmturpei
 */
public class CollectionBase<T> implements Iterable<T> {

    private ArrayList<T> items;

    public CollectionBase() {
        items = new ArrayList<T>();
    }

    /**
     * Adds an object to collection.
     * @param item 
     */
    public void add(T item) {
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
    public void remove(T item) {
        if (item == null) {
            throw new NullPointerException("item was null at CollectionBase.remove");
        }
        items.remove(item);
    }

    /**
     * Return the amount of objects in this collection
     * @return 
     */
    public int size() {
        return items.size();
    }

    /**
     * Return an item with the given index.
     * @param index
     * @return 
     */
    public T getItem(int index) {
        if (index < 0 || index >= items.size()) {
            throw new IndexOutOfBoundsException("index was invalid at CollectionBase.getItem");
        }
        return items.get(index);
    }

    /**
     * Called whenever addItem(Item T) is called.
     * @param item 
     */
    protected void processItem(T item) {
    }

    /**
     * Return an iterator for this collection.
     * @return iterator to collection;
     */
    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }
}
