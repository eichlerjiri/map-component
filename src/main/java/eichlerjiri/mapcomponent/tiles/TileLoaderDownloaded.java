package eichlerjiri.mapcomponent.tiles;

import java.io.File;
import java.io.InterruptedIOException;

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.LoadedTile;
import eichlerjiri.mapcomponent.utils.RequestedTile;

import static eichlerjiri.mapcomponent.utils.Common.*;

public class TileLoaderDownloaded extends TileRunnable {

    public final byte[] data;

    public TileLoaderDownloaded(MapComponent mc, RequestedTile tile, byte[] data) {
        super(mc, tile);
        this.data = data;
    }

    @Override
    public void run() {
        try {
            if (priority != tile.priority) {
                priority = tile.priority;
                mc.tileLoadPool.execute(this);
            } else {
                LoadedTile loadedTile = decodeTile(tile, data);
                mc.tileLoadPool.loadedTiles.add(loadedTile);
                mc.glView.requestRender();

                if (loadedTile.data != null) {
                    File cacheFile = new File(mc.tileLoadPool.cacheDir,
                            "tiles/ " + tile.zoom + "/" + tile.x + "/" + tile.y + ".png");

                    cacheFile.getParentFile().mkdirs();
                    writeFile(cacheFile, data);
                }
            }
        } catch (InterruptedIOException e) {
            // end
        }
    }
}
