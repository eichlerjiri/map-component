package eichlerjiri.mapcomponent.tiles;

import java.io.InterruptedIOException;

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.RequestedTile;

import static eichlerjiri.mapcomponent.utils.Common.*;

public class TileDownloader extends TileRunnable {

    public TileDownloader(MapComponent mc, RequestedTile tile) {
        super(mc, tile);
    }

    @Override
    public void run() {
        try {
            if (tile.cancelled) {
                mc.tileLoadPool.cancelledTiles.add(tile);
            } else if (priority != tile.priority) {
                priority = tile.priority;
                mc.tileDownloadPool.execute(this);
            } else {
                byte[] data = download(mc.tileDownloadPool.getServerUrl() + tile.zoom + "/" + tile.x + "/" + tile.y + ".png");

                mc.tileLoadPool.execute(new TileLoaderDownloaded(mc, tile, data));
            }
        } catch (InterruptedIOException e) {
            // end
        }
    }
}
