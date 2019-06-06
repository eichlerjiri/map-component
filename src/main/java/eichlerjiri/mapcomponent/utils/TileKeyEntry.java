package eichlerjiri.mapcomponent.utils;

public class TileKeyEntry<T> extends TileKey {

    public TileKeyEntry<T> next;
    public T value;

    public TileKeyEntry(int zoom, int x, int y, T value) {
        super(zoom, x, y);
        this.value = value;
    }
}
