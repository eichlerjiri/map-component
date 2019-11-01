package eichlerjiri.mapcomponent.utils;

public class TileKey<T> extends ObjectMapEntry<T> {

    public int zoom;
    public int x;
    public int y;

    public TileKey(int zoom, int x, int y) {
        this.zoom = zoom;
        this.x = x;
        this.y = y;
    }

    public TileKey<T> value(int newZoom, int newX, int newY) {
        zoom = newZoom;
        x = newX;
        y = newY;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        TileKey<T> o = (TileKey<T>) obj;
        return zoom == o.zoom && x == o.x && y == o.y;
    }

    @Override
    public int hashCode() {
        int h = zoom;
        h = 31 * h + x;
        h = 31 * h + y;
        return h;
    }
}
