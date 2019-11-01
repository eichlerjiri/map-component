package eichlerjiri.mapcomponent.utils;

public class RequestedTile extends TileKey<RequestedTile> {

    public int tick;
    public volatile int priority;
    public volatile boolean cancelled;

    public RequestedTile(int zoom, int x, int y, int tick, int priority) {
        super(zoom, x, y);
        this.tick = tick;
        this.priority = priority;
    }
}
