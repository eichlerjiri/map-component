package eichlerjiri.mapcomponent.tiles;

import eichlerjiri.mapcomponent.MapComponent;
import static eichlerjiri.mapcomponent.utils.Common.*;
import eichlerjiri.mapcomponent.utils.RequestedTile;
import java.io.File;
import java.io.InterruptedIOException;

public class TileLoader extends TileRunnable {

    public TileLoader(MapComponent mc, RequestedTile tile) {
        super(mc, tile);
    }

    @Override
    public void run() {
        try {
            if (tile.cancelled.get()) {
                mc.tileLoadPool.cancelledTiles.add(tile);
            } else if (priority.get() != tile.priority.get()) {
                priority.set(tile.priority.get());
                mc.tileLoadPool.execute(this);
            } else {
                File cacheFile = new File(mc.tileLoadPool.cacheDir, "tiles/ " + tile.zoom + "/" + tile.x + "/" + tile.y + ".png");

                if (cacheFile.exists()) {
                    mc.tileLoadPool.loadedTiles.add(decodeTile(tile.zoom, tile.x, tile.y, readFile(cacheFile)));
                    mc.requestRender();
                } else {
                    mc.tileDownloadPool.execute(new TileDownloader(mc, tile));
                }
            }
        } catch (InterruptedIOException e) {
            // end
        }
    }
}
