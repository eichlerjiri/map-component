package eichlerjiri.mapcomponent.utils;

import java.nio.ByteBuffer;

public class LoadedTile extends TileKey<LoadedTile> {

    public final int width;
    public final int height;
    public final ByteBuffer data;

    public LoadedTile(int zoom, int x, int y, int width, int height, ByteBuffer data) {
        super(zoom, x, y);
        this.width = width;
        this.height = height;
        this.data = data;
    }
}
