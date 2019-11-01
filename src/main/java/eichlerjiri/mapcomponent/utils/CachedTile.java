package eichlerjiri.mapcomponent.utils;

public class CachedTile extends TileKey<CachedTile> {

    public final int texture;
    public int tick;

    public CachedTile(int zoom, int x, int y, int texture, int tick) {
        super(zoom, x, y);
        this.texture = texture;
        this.tick = tick;
    }
}
