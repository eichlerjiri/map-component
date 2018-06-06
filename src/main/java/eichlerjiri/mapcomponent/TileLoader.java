package eichlerjiri.mapcomponent;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eichlerjiri.mapcomponent.utils.IOUtils;
import eichlerjiri.mapcomponent.utils.MapTileKey;
import eichlerjiri.mapcomponent.utils.RequestedTile;
import eichlerjiri.mapcomponent.utils.TileRunnable;

public class TileLoader extends ThreadPoolExecutor {

    private final MapComponent mapComponent;
    private final File cacheDir;
    private final HashMap<MapTileKey, RequestedTile> requestedTiles = new HashMap<>();

    public TileLoader(Context c, MapComponent mapComponent) {
        super(1, 1, 0L, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>(),
                new ThreadPoolExecutor.DiscardPolicy());

        this.mapComponent = mapComponent;
        cacheDir = c.getCacheDir();
    }

    public void requestTile(MapTileKey testTileKey, int tick, int priority) {
        RequestedTile previousRunnable = requestedTiles.get(testTileKey);
        if (previousRunnable != null) {
            previousRunnable.tick = tick;
            previousRunnable.cancelled = false;
            return;
        }

        MapTileKey tileKey = new MapTileKey(testTileKey);
        RequestedTile requestedTile = new RequestedTile(tileKey, tick, priority);
        requestedTiles.put(tileKey, requestedTile);
        processTile(requestedTile);
    }

    private void processTile(final RequestedTile requestedTile) {
        execute(new TileRunnable(requestedTile) {
            @Override
            public void run() {
                if (priority != requestedTile.priority) {
                    priority = requestedTile.priority;
                    execute(this);
                    return;
                }

                if (requestedTile.cancelled) {
                    cancelTile(requestedTile);
                } else {
                    loadTile(requestedTile);
                }
            }
        });
    }

    public void cancelUnused(int tick) {
        for (RequestedTile requestedTile : requestedTiles.values()) {
            if (requestedTile.tick != tick) {
                requestedTile.cancelled = true;
            }
        }
    }

    private void loadTile(RequestedTile requestedTile) {
        MapTileKey tileKey = requestedTile.tileKey;
        File cacheFile = new File(cacheDir, "tiles/ " + tileKey.zoom + "/" + tileKey.x + "/" + tileKey.y + ".png");

        if (cacheFile.exists()) {
            byte[] data = IOUtils.readFile(cacheFile);
            processRawImage(requestedTile, data);
        } else {
            mapComponent.tileDownloader.scheduleDownloadTile(requestedTile);
        }
    }

    public void scheduleProcessDownloaded(final RequestedTile requestedTile, final byte[] data) {
        execute(new TileRunnable(requestedTile) {
            @Override
            public void run() {
                if (priority != requestedTile.priority) {
                    priority = requestedTile.priority;
                    execute(this);
                    return;
                }

                if (processRawImage(requestedTile, data)) {
                    saveToCache(requestedTile, data);
                }
            }
        });
    }

    private boolean processRawImage(RequestedTile requestedTile, byte[] data) {
        MapTileKey tileKey = requestedTile.tileKey;

        if (data == null) {
            returnTile(tileKey, 0, 0, null);
            return false;
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bitmap == null) {
            returnTile(tileKey, 0, 0, null);
            return false;
        }

        returnTile(tileKey, bitmap.getWidth(), bitmap.getHeight(), convertBitmap(bitmap));
        return true;
    }

    private void saveToCache(RequestedTile requestedTile, byte[] data) {
        MapTileKey tileKey = requestedTile.tileKey;
        File cacheFile = new File(cacheDir, "tiles/ " + tileKey.zoom + "/" + tileKey.x + "/" + tileKey.y + ".png");

        cacheFile.getParentFile().mkdirs();
        IOUtils.writeFile(cacheFile, data);
    }

    private ByteBuffer convertBitmap(Bitmap b) {
        if (b.getConfig() != Bitmap.Config.ARGB_8888) {
            Log.w("TileLoader", "Converting bitmap");
            b = b.copy(Bitmap.Config.ARGB_8888, false);
        }

        byte[] data = new byte[b.getByteCount()];
        ByteBuffer buffer = ByteBuffer.wrap(data);

        b.copyPixelsToBuffer(buffer);
        buffer.rewind();

        return buffer;
    }

    private void returnTile(final MapTileKey tileKey, final int width, final int height, final ByteBuffer data) {
        mapComponent.queueEventOnDraw(new Runnable() {
            @Override
            public void run() {
                RequestedTile requestedTile = requestedTiles.remove(tileKey);
                if (!requestedTile.cancelled) {
                    mapComponent.renderer.tileLoaded(tileKey, width, height, data);
                }
            }
        });
    }

    public void cancelTile(final RequestedTile requestedTile) {
        mapComponent.glView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (requestedTile.cancelled) {
                    requestedTiles.remove(requestedTile.tileKey);
                } else {
                    processTile(requestedTile);
                }
            }
        });
    }
}
