package eichlerjiri.mapcomponent.tiles;

import android.content.Context;
import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.LoadedTile;
import eichlerjiri.mapcomponent.utils.ObjectMap;
import eichlerjiri.mapcomponent.utils.RequestedTile;
import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TileLoadPool extends ThreadPoolExecutor {

    public final MapComponent mc;
    public final File cacheDir;
    public final ObjectMap<RequestedTile> requestedTiles = new ObjectMap<>(RequestedTile.class);

    public final ConcurrentLinkedQueue<RequestedTile> cancelledTiles = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<LoadedTile> loadedTiles = new ConcurrentLinkedQueue<>();

    public TileLoadPool(Context c, MapComponent mc) {
        super(1, 1, 0L, TimeUnit.SECONDS, new PriorityBlockingQueue<>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = new Thread(runnable);
                t.setPriority(4);
                return t;
            }
        }, new ThreadPoolExecutor.DiscardPolicy());

        this.mc = mc;
        cacheDir = c.getCacheDir();
    }

    public void processLoaded() {
        RequestedTile cancelledTile;
        while ((cancelledTile = cancelledTiles.poll()) != null) {
            if (cancelledTile.cancelled.get()) {
                requestedTiles.remove(cancelledTile);
            } else {
                execute(new TileLoader(mc, cancelledTile));
            }
        }

        LoadedTile loadedTile;
        while ((loadedTile = loadedTiles.poll()) != null) {
            RequestedTile requestedTile = requestedTiles.remove(loadedTile);
            if (!requestedTile.cancelled.get()) {
                mc.renderer.tileLoaded(loadedTile);
            }
        }
    }

    public void requestTile(int z, int x, int y, int tick, int priority) {
        RequestedTile previousRunnable = requestedTiles.get(mc.renderer.tkey.value(z, x, y));

        if (previousRunnable != null) {
            previousRunnable.tick = tick;
            previousRunnable.priority.set(priority);
            previousRunnable.cancelled.set(false);
        } else {
            RequestedTile requestedTile = new RequestedTile(z, x, y, tick, priority);
            requestedTiles.put(requestedTile);

            execute(new TileLoader(mc, requestedTile));
        }
    }

    public void cancelUnused(int tick) {
        for (int i = 0; i < requestedTiles.data.length; i++) {
            RequestedTile entry = requestedTiles.data[i];
            while (entry != null) {
                if (entry.tick != tick) {
                    entry.cancelled.set(true);
                }
                entry = entry.next;
            }
        }
    }
}
