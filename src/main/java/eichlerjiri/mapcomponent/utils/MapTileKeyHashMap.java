package eichlerjiri.mapcomponent.utils;

import java.util.Arrays;

public class MapTileKeyHashMap<T> {

    public MapTileKey[] keys = new MapTileKey[16];
    public Object[] values = new Object[16];
    public int size;

    public T get(int zoom, int x, int y) {
        return (T) values[index(zoom, x, y)];
    }

    public void put(MapTileKey key, T value) {
        int index = index(key.zoom, key.x, key.y);
        MapTileKey origKey = keys[index];

        keys[index] = key;
        values[index] = value;

        if (origKey == null) {
            size++;
            if (size * 2 > keys.length) {
                enlarge();
            }
        }
    }

    public T remove(MapTileKey key) {
        return remove(index(key.zoom, key.x, key.y));
    }

    public T remove(int index) {
        MapTileKey key = keys[index];
        if (key == null) {
            return null;
        }
        Object ret = values[index];

        keys[index] = null;
        values[index] = null;
        size--;

        index++;
        if (index >= keys.length) {
            index = 0;
        }

        while ((key = keys[index]) != null) {
            Object value = values[index];
            keys[index] = null;
            values[index] = null;

            int newIndex = index(key.zoom, key.x, key.y);
            keys[newIndex] = key;
            values[newIndex] = value;

            index++;
            if (index >= keys.length) {
                index = 0;
            }
        }
        return (T) ret;
    }

    public void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        size = 0;
    }

    public int index(int zoom, int x, int y) {
        int hash = 31 * (31 * zoom + x) + y;
        int index = hash & (keys.length - 1);

        MapTileKey key;
        while ((key = keys[index]) != null && (key.zoom != zoom || key.x != x || key.y != y)) {
            index++;
            if (index >= keys.length) {
                index = 0;
            }
        }
        return index;
    }

    private void enlarge() {
        MapTileKey[] oldKeys = keys;
        Object[] oldValues = values;
        keys = new MapTileKey[oldKeys.length * 2];
        values = new Object[oldValues.length * 2];

        for (int i = 0; i < oldKeys.length; i++) {
            MapTileKey key = oldKeys[i];
            if (key != null) {
                int index = index(key.zoom, key.x, key.y);
                keys[index] = key;
                values[index] = oldValues[i];
            }
        }
    }
}
