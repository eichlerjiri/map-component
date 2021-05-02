package eichlerjiri.mapcomponent.utils;

import java.lang.reflect.Array;

public class ObjectList<T> {

    public T[] data;
    public int size;

    public ObjectList(Class<T> clazz) {
        data = (T[]) Array.newInstance(clazz, 8);
    }

    public void add(T v) {
        if (data.length < size + 1) {
            zz_ensureCapacity(1);
        }

        data[size++] = v;
    }

    public void remove(T v) {
        for (int i = 0; i < size; i++) {
            if (data[i] == v) {
                size--;
                for (; i < size; i++) {
                    data[i] = data[i + 1];
                }
                data[size] = null;
                return;
            }
        }
    }

    public void zz_ensureCapacity(int extra) {
        int newCapacity = data.length * 2;
        while (newCapacity < size + extra) {
            newCapacity *= 2;
        }

        T[] dataNew = (T[]) Array.newInstance(data.getClass().getComponentType(), newCapacity);
        System.arraycopy(data, 0, dataNew, 0, size);
        data = dataNew;
    }
}
