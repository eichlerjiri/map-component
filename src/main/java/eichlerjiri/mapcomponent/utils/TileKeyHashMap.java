package eichlerjiri.mapcomponent.utils;

public class TileKeyHashMap<T> {

    public Entry<T>[] entries = new Entry[16];
    public int size;
    private int resizeAfter = 12;

    public T get(int zoom, int x, int y) {
        int index = (31 * (31 * (31 * zoom + x) + y)) & (entries.length - 1);

        Entry<T> entry = entries[index];
        while (entry != null) {
            if (entry.zoom == zoom && entry.x == x && entry.y == y) {
                return entry.value;
            }
            entry = entry.next;
        }
        return null;
    }

    public void put(TileKey key, T value) {
        int index = (31 * (31 * (31 * key.zoom + key.x) + key.y)) & (entries.length - 1);

        Entry<T> prev = null;
        Entry<T> entry = entries[index];
        while (entry != null) {
            if (entry.zoom == key.zoom && entry.x == key.x && entry.y == key.y) {
                entry.value = value;
                return;
            }
            prev = entry;
            entry = entry.next;
        }

        entry = new Entry<>(key.zoom, key.x, key.y, value);
        if (prev != null) {
            prev.next = entry;
        } else {
            entries[index] = entry;
        }

        size++;
        if (size > resizeAfter) {
            enlarge();
        }
    }

    public T remove(TileKey key) {
        int index = (31 * (31 * (31 * key.zoom + key.x) + key.y)) & (entries.length - 1);

        Entry<T> prev = null;
        Entry<T> entry = entries[index];
        while (entry != null) {
            if (entry.zoom == key.zoom && entry.x == key.x && entry.y == key.y) {
                if (prev != null) {
                    prev.next = entry.next;
                } else {
                    entries[index] = entry.next;
                }
                size--;
                return entry.value;
            }
            prev = entry;
            entry = entry.next;
        }
        return null;
    }

    public void clear() {
        for (int i = 0; i < entries.length; i++) {
            entries[i] = null;
        }
        size = 0;
    }

    private void enlarge() {
        Entry<T>[] oldKeys = entries;
        entries = new Entry[oldKeys.length * 2];
        resizeAfter *= 2;

        for (Entry<T> entry : oldKeys) {
            while (entry != null) {
                Entry<T> next = entry.next;

                int index = (31 * (31 * (31 * entry.zoom + entry.x) + entry.y)) & (entries.length - 1);
                entry.next = entries[index];
                entries[index] = entry;

                entry = next;
            }
        }
    }

    public static class Entry<T> extends TileKey {
        public Entry<T> next;
        public T value;

        public Entry(int zoom, int x, int y, T value) {
            super(zoom, x, y);
            this.value = value;
        }
    }
}
