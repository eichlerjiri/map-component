package eichlerjiri.mapcomponent.utils;

public class RequestedTile {

    public final MapTileKey tileKey;
    public int tick;
    public volatile int priority;
    public volatile boolean cancelled;

    public RequestedTile(MapTileKey tileKey, int tick, int priority) {
        this.tileKey = tileKey;
        this.tick = tick;
        this.priority = priority;
    }
}
