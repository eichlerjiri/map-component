package eichlerjiri.mapcomponent.utils;

import java.nio.ByteBuffer;

public class LoadedTile extends TileKey {

    public final int width;
    public final int height;
    public final ByteBuffer data;

    public LoadedTile(TileKey tileKey, int width, int height, ByteBuffer data) {
        super(tileKey.zoom, tileKey.x, tileKey.y);
        this.width = width;
        this.height = height;
        this.data = data;
    }
}
