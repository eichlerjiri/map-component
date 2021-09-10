package eichlerjiri.mapcomponent.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestedTile extends TileKey<RequestedTile> {

    public int tick;
    public AtomicInteger priority;
    public AtomicBoolean cancelled = new AtomicBoolean();

    public RequestedTile(int zoom, int x, int y, int tick, int priority) {
        super(zoom, x, y);
        this.tick = tick;
        this.priority = new AtomicInteger(priority);
    }
}
