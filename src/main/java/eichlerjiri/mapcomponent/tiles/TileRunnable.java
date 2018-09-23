package eichlerjiri.mapcomponent.tiles;

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.RequestedTile;

public abstract class TileRunnable implements Runnable, Comparable<TileRunnable> {

    public final MapComponent mc;
    public final RequestedTile tile;
    public volatile int priority;

    public TileRunnable(MapComponent mc, RequestedTile tile) {
        this.mc = mc;
        this.tile = tile;
        priority = tile.priority;
    }

    @Override
    public int compareTo(TileRunnable o) {
        return priority - o.priority;
    }
}
