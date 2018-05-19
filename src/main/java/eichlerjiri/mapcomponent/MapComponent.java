package eichlerjiri.mapcomponent;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;

public class MapComponent extends GLSurfaceView {

    public final MapComponentRenderer renderer = new MapComponentRenderer(this);
    public final TileLoader tileLoader;

    private float lastX1 = Float.MIN_VALUE;
    private float lastY1 = Float.MIN_VALUE;
    private float lastX2 = Float.MIN_VALUE;
    private float lastY2 = Float.MIN_VALUE;

    public MapComponent(Context context, ArrayList<String> mapUrls) {
        super(context);
        tileLoader = new TileLoader(context, this, mapUrls);

        setEGLContextClientVersion(2);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int index = event.getActionIndex();
            int id = event.getPointerId(index);
            float x = event.getX(index);
            float y = event.getY(index);

            if (id == 0) {
                lastX1 = x;
                lastY1 = y;
            } else if (id == 1) {
                lastX2 = x;
                lastY2 = y;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            int id = event.getPointerId(event.getActionIndex());

            if (id == 0) {
                lastX1 = Float.MIN_VALUE;
                lastY1 = Float.MIN_VALUE;
            } else if (id == 1) {
                lastX2 = Float.MIN_VALUE;
                lastY2 = Float.MIN_VALUE;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            float preDist = computeDistance();

            int pointerCount = event.getPointerCount();
            for (int i = 0; i < pointerCount; i++) {
                int id = event.getPointerId(i);
                float x = event.getX(i);
                float y = event.getY(i);

                if (id == 0) {
                    if (lastX2 == Float.MIN_VALUE) {
                        final float diffX = lastX1 - x;
                        final float diffY = lastY1 - y;

                        queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                renderer.moveByPixels(diffX, diffY);
                            }
                        });
                    }

                    lastX1 = x;
                    lastY1 = y;
                } else if (id == 1) {
                    lastX2 = x;
                    lastY2 = y;
                }
            }

            float postDist = computeDistance();

            if (preDist != 0 && postDist != 0) {
                float zoomChange = (postDist / preDist) - 1;
                float centerX = (lastX1 + lastX2) / 2;
                float centerY = (lastY1 + lastY2) / 2;
                renderer.changeZoom(zoomChange, centerX, centerY);
            }
        }

        return true;
    }

    private float computeDistance() {
        if (lastX1 == Float.MIN_VALUE || lastX2 == Float.MIN_VALUE) {
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
