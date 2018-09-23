package eichlerjiri.mapcomponent.tiles;

import java.io.File;
import java.io.InterruptedIOException;

import eichlerjiri.mapcomponent.MapComponent;
import eichlerjiri.mapcomponent.utils.LoadedTile;
import eichlerjiri.mapcomponent.utils.MapTileKey;
import eichlerjiri.mapcomponent.utils.RequestedTile;

import static eichlerjiri.mapcomponent.utils.Common.*;

public class TileLoaderDownloaded extends TileRunnable {

    private final byte[] data;

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
                MapTileKey tileKey = tile.tileKey;

                LoadedTile loadedTile = decodeTile(tileKey, data);
                mc.tileLoadPool.loadedTiles.add(loadedTile);
                mc.glView.requestRender();

                if (loadedTile.data != null) {
                    File cacheFile = new File(mc.tileLoadPool.cacheDir,
                            "tiles/ " + tileKey.zoom + "/" + tileKey.x + "/" + tileKey.y + ".png");

                    cacheFile.getParentFile().mkdirs();
                    writeFile(cacheFile, data);
                }
            }
        } catch (InterruptedIOException e) {
            // end
        }
    }
}
