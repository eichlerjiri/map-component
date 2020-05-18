package eichlerjiri.mapcomponent.utils;

import java.lang.reflect.Array;

public class ObjectMap<T extends ObjectMapEntry<T>> {

    public T[] data;
    public int size;
    public int resizeAfter = 12;

    public ObjectMap(Class<?> clazz) {
        data = (T[]) Array.newInstance(clazz, 8);
    }

    public T get(Object key) {
        int index = key.hashCode() & (data.length - 1);

        T entry = data[index];
        while (entry != null) {
            if (entry.equals(key)) {
                return entry;
            }
            entry = entry.next;
        }
        return null;
    }

    public void put(T item) {
        int index = item.hashCode() & (data.length - 1);

        T prev = null;
        T entry = data[index];
        while (entry != null && !entry.equals(item)) {
            prev = entry;
            entry = entry.next;
        }

        item.next = entry;

        if (prev != null) {
            prev.next = item;
        } else {
            data[index] = item;
        }

        size++;
        if (size > resizeAfter) {
            enlarge();
        }
    }

    public T remove(Object key) {
        int index = key.hashCode() & (data.length - 1);

        T prev = null;
        T entry = data[index];
        while (entry != null) {
            if (entry.equals(key)) {
                if (prev != null) {
                    prev.next = entry.next;
                } else {
                    data[index] = entry.next;
                }
                size--;
                return entry;
            }
            prev = entry;
            entry = entry.next;
        }
        return null;
    }

    public void enlarge() {
        T[] oldData = data;
        data = (T[]) Array.newInstance(oldData.getClass().getComponentType(), oldData.length * 2);
        resizeAfter *= 2;

        for (int i = 0; i < oldData.length; i++) {
            T entry = oldData[i];
            while (entry != null) {
                T next = entry.next;

                int index = entry.hashCode() & (data.length - 1);
                entry.next = data[index];
                data[index] = entry;

                entry = next;
            }
        }
    }
}
