package eichlerjiri.mapcomponent.tiles;

import eichlerjiri.mapcomponent.MapComponent;
import static eichlerjiri.mapcomponent.utils.Common.*;
import eichlerjiri.mapcomponent.utils.TileKey.RequestedTile;
import java.io.InterruptedIOException;

public class TileDownloader extends TileRunnable {

    public TileDownloader(MapComponent mc, RequestedTile tile) {
        super(mc, tile);
    }

    @Override
    public void run() {
        try {
            if (tile.cancelled.get()) {
                mc.tileLoadPool.cancelledTiles.add(tile);
            } else if (priority.get() != tile.priority.get()) {
                priority.set(tile.priority.get());
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
