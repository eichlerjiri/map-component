package eichlerjiri.mapcomponent.utils;

import eichlerjiri.mapcomponent.utils.ObjectMap.ObjectMapEntry;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static class CachedTile extends TileKey<CachedTile> {

        public final int texture;
        public int tick;

        public CachedTile(int zoom, int x, int y, int texture, int tick) {
            super(zoom, x, y);
            this.texture = texture;
            this.tick = tick;
        }
    }

    public static class LoadedTile extends TileKey<LoadedTile> {

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

    public static class RequestedTile extends TileKey<RequestedTile> {

        public int tick;
        public AtomicInteger priority;
        public AtomicBoolean cancelled = new AtomicBoolean();

        public RequestedTile(int zoom, int x, int y, int tick, int priority) {
            super(zoom, x, y);
            this.tick = tick;
            this.priority = new AtomicInteger(priority);
        }
    }
}
