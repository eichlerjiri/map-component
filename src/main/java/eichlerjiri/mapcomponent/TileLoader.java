package eichlerjiri.mapcomponent;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eichlerjiri.mapcomponent.utils.IOUtils;
import eichlerjiri.mapcomponent.utils.MapTileKey;

public class TileLoader extends ThreadPoolExecutor {

    private final MapComponent mapComponent;
    private final ArrayList<String> mapUrls;
    private final File cacheDir;
    private final HashMap<MapTileKey, TileRunnable> requestedTiles = new HashMap<>();

    public TileLoader(Context c, MapComponent mapComponent, ArrayList<String> mapUrls) {
        super(10, 10, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        this.mapComponent = mapComponent;
        this.mapUrls = mapUrls;
        cacheDir = c.getCacheDir();
    }

    public void requestTile(MapTileKey testTileKey, int tick) {
        TileRunnable previousRunnable = requestedTiles.get(testTileKey);
        if (previousRunnable != null) {
            previousRunnable.tick(tick);
            return;
        }

        MapTileKey tileKey = new MapTileKey(testTileKey);
        TileRunnable tileRunnable = new TileRunnable(tileKey, tick);
        requestedTiles.put(tileKey, tileRunnable);
        execute(tileRunnable);
    }

    public void cancelUnused(int tick) {
        for (TileRunnable runnable : requestedTiles.values()) {
            if (runnable.tick != tick) {
                runnable.cancelled = true;
            }
        }
    }

    private void loadTile(MapTileKey tileKey) {
        File cacheFile = new File(cacheDir, "tiles/ " + tileKey.zoom + "/" + tileKey.x + "/" + tileKey.y + ".png");
        boolean cacheFileExists = cacheFile.exists();

        byte[] data = null;
        if (cacheFileExists) {
            data = IOUtils.readFile(cacheFile);
        }
        if (data == null) {
            String url = mapUrls.get(new Random().nextInt(mapUrls.size())) +
                tileKey.zoom + "/" + tileKey.x + "/" + tileKey.y + ".png";
            Log.i("TileLoader", "Downloading: " + url);
            data = IOUtils.download(url);
        }

        if (data == null) {
            returnTile(tileKey, 0, 0, null);
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bitmap == null) {
            returnTile(tileKey, 0, 0, null);
            return;
        }

        if (!cacheFileExists) {
            cacheFile.getParentFile().mkdirs();
            IOUtils.writeFile(cacheFile, data);
        }

        returnTile(tileKey, bitmap.getWidth(), bitmap.getHeight(), convertBitmap(bitmap));
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
        mapComponent.queueEvent(new Runnable() {
            @Override
            public void run() {
                TileRunnable tileRunnable = requestedTiles.remove(tileKey);
                if (!tileRunnable.cancelled) {
                    mapComponent.renderer.tileLoaded(tileKey, width, height, data);
                }
            }
        });
    }

    private void cancelTile(final TileRunnable tileRunnable) {
        mapComponent.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (tileRunnable.cancelled) {
                    requestedTiles.remove(tileRunnable.tileKey);
                } else {
                    execute(tileRunnable);
                }
            }
        });
    }

    private class TileRunnable implements Runnable {

        public final MapTileKey tileKey;
        public volatile boolean cancelled;
        public int tick;

        public TileRunnable(MapTileKey tileKey, int tick) {
            this.tileKey = tileKey;
            this.tick = tick;
        }

        public void tick(int tick) {
            cancelled = false;
            this.tick = tick;
        }

        @Override
        public void run() {
            if (cancelled) {
                cancelTile(this);
            } else {
                loadTile(tileKey);
            }
        }
    }
}
