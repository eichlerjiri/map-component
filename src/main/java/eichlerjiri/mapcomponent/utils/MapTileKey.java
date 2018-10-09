package eichlerjiri.mapcomponent.utils;

public class MapTileKey {

    public final int zoom;
    public final int x;
    public final int y;

    public MapTileKey(int zoom, int x, int y) {
        this.zoom = zoom;
        this.x = x;
        this.y = y;
    }
}
