package eichlerjiri.mapcomponent;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import java.util.ArrayList;

public class MapComponent extends GLSurfaceView {

    public final MapComponentRenderer renderer = new MapComponentRenderer(this);
    public final TileLoader tileLoader;

    private float lastX;
    private float lastY;

    public MapComponent(Context context, ArrayList<String> mapUrls) {
        super(context);
        tileLoader = new TileLoader(context, this, mapUrls);

        setEGLContextClientVersion(2);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            lastX = x;
            lastY = y;
        } else if (action == MotionEvent.ACTION_MOVE) {
            final float diffX = lastX - x;
            final float diffY = lastY - y;
            lastX = x;
            lastY = y;

            queueEvent(new Runnable() {
                @Override
                public void run() {
                    renderer.moveByPixels(diffX, diffY);
                }
            });
        }
        return true;
    }

    public void close() {
        tileLoader.shutdownNow();
    }
}
