package eichlerjiri.mapcomponent;

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eichlerjiri.mapcomponent.utils.MapTileKey;

public class TileLoader extends ThreadPoolExecutor {

    private final MapComponentRenderer renderer;
    private final HashSet<MapTileKey> requestedTiles = new HashSet<>();

    public TileLoader(MapComponentRenderer renderer) {
        super(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        this.renderer = renderer;
    }

    public void requestTile(final MapTileKey tileKey) {
        if (requestedTiles.contains(tileKey)) {
            return;
        }
        requestedTiles.add(tileKey);

        submit(new Runnable() {
            @Override
            public void run() {
                loadTile(tileKey);
            }
        });
    }

    private void loadTile(final MapTileKey tileKey) {
        // TODO


        renderer.getMapComponent().queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.tileLoaded(tileKey, null);
            }
        });

    }
}
