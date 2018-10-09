package eichlerjiri.mapcomponent.tiles;

import android.content.Context;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.LoadedTile;
import eichlerjiri.mapcomponent.utils.MapTileKey;
import eichlerjiri.mapcomponent.utils.MapTileKeyHashMap;
import eichlerjiri.mapcomponent.utils.RequestedTile;

public class TileLoadPool extends ThreadPoolExecutor {

    public final MapComponent mc;
    public final File cacheDir;
    private final MapTileKeyHashMap<RequestedTile> requestedTiles = new MapTileKeyHashMap<>();

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
                requestedTiles.remove(cancelledTile.tileKey);
            } else {
                execute(new TileLoader(mc, cancelledTile));
            }
        }

        LoadedTile loadedTile;
        while ((loadedTile = loadedTiles.poll()) != null) {
            RequestedTile requestedTile = requestedTiles.remove(loadedTile.tileKey);
            if (!requestedTile.cancelled) {
                mc.renderer.tileLoaded(loadedTile);
            }
        }
    }

    public void requestTile(int z, int x, int y, int tick, int priority) {
        RequestedTile previousRunnable = requestedTiles.get(z, x, y);

        if (previousRunnable != null) {
            previousRunnable.tick = tick;
            previousRunnable.cancelled = false;
        } else {
            MapTileKey tileKey = new MapTileKey(z, x, y);
            RequestedTile requestedTile = new RequestedTile(tileKey, tick, priority);
            requestedTiles.put(tileKey, requestedTile);

            execute(new TileLoader(mc, requestedTile));
        }
    }

    public void cancelUnused(int tick) {
        for (Object o : requestedTiles.values) {
            if (o != null) {
                RequestedTile tile = (RequestedTile) o;
                if (tile.tick != tick) {
                    tile.cancelled = true;
                }
            }
        }
    }
}
