package eichlerjiri.mapcomponent;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;

public class MapComponent extends GLSurfaceView {

    public final MapComponentRenderer renderer = new MapComponentRenderer(this);
    public final TileLoader tileLoader;

    private int id1 = -1;
    private float lastX1;
    private float lastY1;

    private int id2 = -1;
    private float lastX2;
    private float lastY2;

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
        int pointerCount = event.getPointerCount();

        for (int i = 0; i < pointerCount; i++) {
            int id = event.findPointerIndex(i);
            float x = event.getX(i);
            float y = event.getY(i);

            if (action == MotionEvent.ACTION_DOWN) {
                if (id1 == -1) {
                    id1 = id;
                    Log.i("MapComponent", "id1 down: " + id);
                    lastX1 = x;
                    lastY1 = y;
                } else if (id2 == -1) {
                    id2 = id;
                    Log.i("MapComponent", "id2 down: " + id);
                    lastX2 = x;
                    lastY2 = y;
                }
            } else if (action == MotionEvent.ACTION_UP) {
                if (id1 == id) {
                    id1 = -1;
                    Log.i("MapComponent", "id1 up: " + id);
                } else if (id2 == id) {
                    id2 = -1;
                    Log.i("MapComponent", "id2 up: " + id);
                }
            } else if (action == MotionEvent.ACTION_MOVE) {
                float preDist = computeDistance();

                if (id1 == id) {
                    final float diffX = lastX1 - x;
                    final float diffY = lastY1 - y;
                    lastX1 = x;
                    lastY1 = y;

                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            renderer.moveByPixels(diffX, diffY);
                        }
                    });
                } else if (id2 == id) {
                    lastX2 = x;
                    lastY2 = y;
                }

                float postDist = computeDistance();

                if (preDist != 0 && postDist != 0) {
                    float zoomChange = (postDist / preDist) - 1;
                 //   Log.e("MapComopnent", "zoomChange: " + zoomChange);
                }
            }
        }
        return true;
    }

    private float computeDistance() {
        if (id1 == -1 || id2 == -1) {
            return 0;
        }
        float xDiff = Math.abs(lastX1 - lastX2);
        float yDiff = Math.abs(lastY1 - lastY2);
        return (float) Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    public void close() {
        tileLoader.shutdownNow();
    }
}
