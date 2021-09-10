package eichlerjiri.mapcomponent.tiles;

import eichlerjiri.mapcomponent.MapComponent;
import static eichlerjiri.mapcomponent.utils.Common.*;
import eichlerjiri.mapcomponent.utils.LoadedTile;
import eichlerjiri.mapcomponent.utils.RequestedTile;
import java.io.File;
import java.io.InterruptedIOException;

public class TileLoaderDownloaded extends TileRunnable {

    public final byte[] data;

    public TileLoaderDownloaded(MapComponent mc, RequestedTile tile, byte[] data) {
        super(mc, tile);
        this.data = data;
    }

    @Override
    public void run() {
        try {
            if (priority.get() != tile.priority.get()) {
                priority.set(tile.priority.get());
                mc.tileLoadPool.execute(this);
            } else {
                LoadedTile loadedTile = decodeTile(tile.zoom, tile.x, tile.y, data);
                mc.tileLoadPool.loadedTiles.add(loadedTile);
                mc.requestRender();

                if (loadedTile.data != null) {
                    File cacheFile = new File(mc.tileLoadPool.cacheDir, "tiles/ " + tile.zoom + "/" + tile.x + "/" + tile.y + ".png");

                    cacheFile.getParentFile().mkdirs();
                    writeFile(cacheFile, data);
                }
            }
        } catch (InterruptedIOException e) {
            // end
        }
    }
}
