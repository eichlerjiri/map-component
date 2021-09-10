package eichlerjiri.mapcomponent.tiles;

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.TileKey.RequestedTile;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class TileRunnable implements Runnable, Comparable<TileRunnable> {

    public final MapComponent mc;
    public final RequestedTile tile;
    public AtomicInteger priority;

    public TileRunnable(MapComponent mc, RequestedTile tile) {
        this.mc = mc;
        this.tile = tile;
        priority = new AtomicInteger(tile.priority.get());
    }

    @Override
    public int compareTo(TileRunnable o) {
        return priority.get() - o.priority.get();
    }
}
