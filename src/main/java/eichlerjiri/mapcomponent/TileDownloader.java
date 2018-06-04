package eichlerjiri.mapcomponent;

import android.util.Log;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eichlerjiri.mapcomponent.utils.IOUtils;
import eichlerjiri.mapcomponent.utils.MapTileKey;
import eichlerjiri.mapcomponent.utils.RequestedTile;
import eichlerjiri.mapcomponent.utils.TileRunnable;

public class TileDownloader extends ThreadPoolExecutor {

    private final TileLoader tileLoader;
    private final ArrayList<String> mapUrls;
    private final ThreadLocal<String> serverUrl = new ThreadLocal<>();

    public TileDownloader(TileLoader tileLoader, ArrayList<String> mapUrls) {
        super(10, 10, 10L, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>());
        allowCoreThreadTimeOut(true);

        this.tileLoader = tileLoader;
        this.mapUrls = mapUrls;
    }

    public void scheduleDownloadTile(final RequestedTile requestedTile) {
        execute(new TileRunnable(requestedTile) {
            @Override
            public void run() {
                if (priority != requestedTile.priority) {
                    priority = requestedTile.priority;
                    execute(this);
                    return;
                }

                if (requestedTile.cancelled) {
                    tileLoader.cancelTile(requestedTile);
                } else {
                    downloadTile(requestedTile);
                }
            }
        });
    }

    private void downloadTile(RequestedTile tile) {
        MapTileKey tileKey = tile.tileKey;

        String serverUrlStr = serverUrl.get();
        if (serverUrlStr == null) {
            serverUrlStr = mapUrls.get(new Random().nextInt(mapUrls.size()));
            serverUrl.set(serverUrlStr);
        }

        String url = serverUrlStr + tileKey.zoom + "/" + tileKey.x + "/" + tileKey.y + ".png";
        Log.i("TileDownloader", "Downloading: " + url);
        byte[] data = IOUtils.download(url);

        tileLoader.scheduleProcessDownloaded(tile, data);
    }
}
