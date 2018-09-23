package eichlerjiri.mapcomponent.utils;

import java.nio.ByteBuffer;

public class LoadedTile {

    public final MapTileKey tileKey;
    public final int width;
    public final int height;
    public final ByteBuffer data;

    public LoadedTile(MapTileKey tileKey, int width, int height, ByteBuffer data) {
        this.tileKey = tileKey;
        this.width = width;
        this.height = height;
        this.data = data;
    }
}
