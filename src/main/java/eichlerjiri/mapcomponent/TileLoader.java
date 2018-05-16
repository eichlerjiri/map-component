package eichlerjiri.mapcomponent;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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
    private final HashSet<MapTileKey> requestedTiles = new HashSet<>();

    public TileLoader(Context c, MapComponent mapComponent, ArrayList<String> mapUrls) {
        super(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        this.mapComponent = mapComponent;
        this.mapUrls = mapUrls;
        cacheDir = c.getCacheDir();
    }

    public void requestTile(final MapTileKey tileKey) {
        if (requestedTiles.contains(tileKey)) {
            return;
        }
        requestedTiles.add(tileKey);

        execute(new Runnable() {
            @Override
            public void run() {
                loadTile(tileKey);
            }
        });
    }

    private void loadTile(MapTileKey tileKey) {
        File cacheFile = new File(cacheDir, "tiles/ " + tileKey.zoom + "/" + tileKey.x + "/" + tileKey.y + ".png");
        boolean cacheFileExists = cacheFile.exists();

        byte[] data;
        if (cacheFileExists) {
            data = IOUtils.readFile(cacheFile);
        } else {
            data = IOUtils.download(mapUrls.get(new Random().nextInt(mapUrls.size())) +
                    tileKey.zoom + "/" + tileKey.x + "/" + tileKey.y + ".png");
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
                requestedTiles.remove(tileKey);

                mapComponent.renderer.tileLoaded(tileKey, width, height, data);
            }
        });
    }
}
