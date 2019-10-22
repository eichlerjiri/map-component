package eichlerjiri.mapcomponent.utils;

public class CachedTile {

    public final int texture;
    public int tick;

    public CachedTile(int textureId, int tick) {
        this.texture = textureId;
        this.tick = tick;
    }
}
