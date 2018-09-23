package eichlerjiri.mapcomponent.tiles;

import java.io.File;
import java.io.InterruptedIOException;

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.MapTileKey;
import eichlerjiri.mapcomponent.utils.RequestedTile;

import static eichlerjiri.mapcomponent.utils.Common.*;

public class TileLoader extends TileRunnable {

    public TileLoader(MapComponent mc, RequestedTile tile) {
        super(mc, tile);
    }

    @Override
    public void run() {
        try {
            if (tile.cancelled) {
                mc.tileLoadPool.cancelledTiles.add(tile);
            } else if (priority != tile.priority) {
                priority = tile.priority;
                mc.tileLoadPool.execute(this);
            } else {
                MapTileKey tileKey = tile.tileKey;
                File cacheFile = new File(mc.tileLoadPool.cacheDir,
                        "tiles/ " + tileKey.zoom + "/" + tileKey.x + "/" + tileKey.y + ".png");

                if (cacheFile.exists()) {
                    mc.tileLoadPool.loadedTiles.add(decodeTile(tileKey, readFile(cacheFile)));
                    mc.glView.requestRender();
                } else {
                    mc.tileDownloadPool.execute(new TileDownloader(mc, tile));
                }
            }
        } catch (InterruptedIOException e) {
            // end
        }
    }
}
