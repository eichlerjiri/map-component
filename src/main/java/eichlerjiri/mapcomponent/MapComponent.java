package eichlerjiri.mapcomponent;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;

import java.util.ArrayList;

public class MapComponent extends GLSurfaceView {

    public final MapComponentRenderer renderer = new MapComponentRenderer(this);
    public final TileLoader tileLoader;

    public MapComponent(Context context, ArrayList<String> mapUrls) {
        super(context);
        tileLoader = new TileLoader(context, this, mapUrls);

        setEGLContextClientVersion(2);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void close() {
        tileLoader.shutdownNow();
    }
}
