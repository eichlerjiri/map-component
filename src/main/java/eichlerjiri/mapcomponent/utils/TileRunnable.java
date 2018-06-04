package eichlerjiri.mapcomponent.utils;

public abstract class TileRunnable implements Runnable, Comparable<TileRunnable> {

    public final RequestedTile requestedTile;
    public volatile int priority;

    public TileRunnable(RequestedTile requestedTile) {
        this.requestedTile = requestedTile;
        priority = requestedTile.priority;
    }

    @Override
    public int compareTo(TileRunnable o) {
        return priority - o.priority;
    }
}
