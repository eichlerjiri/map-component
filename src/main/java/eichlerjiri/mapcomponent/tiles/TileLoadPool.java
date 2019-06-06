package eichlerjiri.mapcomponent.tiles;

import android.content.Context;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.LoadedTile;
import eichlerjiri.mapcomponent.utils.RequestedTile;
import eichlerjiri.mapcomponent.utils.TileKeyEntry;
import eichlerjiri.mapcomponent.utils.TileKeyHashMap;

public class TileLoadPool extends ThreadPoolExecutor {

    public final MapComponent mc;
    public final File cacheDir;
    public final TileKeyHashMap<RequestedTile> requestedTiles = new TileKeyHashMap<>();

    public final ConcurrentLinkedQueue<RequestedTile> cancelledTiles = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<LoadedTile> loadedTiles = new ConcurrentLinkedQueue<>();

    public TileLoadPool(Context c, MapComponent mc) {
        super(1, 1, 0L, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>(),
                new ThreadPoolExecutor.DiscardPolicy());

        this.mc = mc;
        cacheDir = c.getCacheDir();
    }

    public void processLoaded() {
        RequestedTile cancelledTile;
        while ((cancelledTile = cancelledTiles.poll()) != null) {
            if (cancelledTile.cancelled) {
                requestedTiles.remove(cancelledTile);
            } else {
                execute(new TileLoader(mc, cancelledTile));
            }
        }

        LoadedTile loadedTile;
        while ((loadedTile = loadedTiles.poll()) != null) {
            RequestedTile requestedTile = requestedTiles.remove(loadedTile);
            if (!requestedTile.cancelled) {
                mc.renderer.tileLoaded(loadedTile);
            }
        }
    }

    public void requestTile(int z, int x, int y, int tick, int priority) {
        RequestedTile previousRunnable = requestedTiles.get(z, x, y);

        if (previousRunnable != null) {
            previousRunnable.tick = tick;
            previousRunnable.priority = priority;
            previousRunnable.cancelled = false;
        } else {
            RequestedTile requestedTile = new RequestedTile(z, x, y, tick, priority);
            requestedTiles.put(requestedTile, requestedTile);

            execute(new TileLoader(mc, requestedTile));
        }
    }

    public void cancelUnused(int tick) {
        for (int i = 0; i < requestedTiles.entries.length; i++) {
            TileKeyEntry<RequestedTile> entry = requestedTiles.entries[i];
            while (entry != null) {
                RequestedTile tile = entry.value;
                if (tile.tick != tick) {
                    tile.cancelled = true;
                }
                entry = entry.next;
            }
        }
    }
}
