package eichlerjiri.mapcomponent;

import android.content.Context;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import eichlerjiri.mapcomponent.utils.CurrentPosition;
import eichlerjiri.mapcomponent.utils.MercatorUtils;
import eichlerjiri.mapcomponent.utils.Position;

public class MapComponent extends GLSurfaceView {

    public final MapComponentRenderer renderer = new MapComponentRenderer(this);
    public final TileLoader tileLoader;

    public final ConcurrentLinkedQueue<Runnable> onDrawRunnables = new ConcurrentLinkedQueue<>();

    private float lastX1 = Float.MIN_VALUE;
    private float lastY1 = Float.MIN_VALUE;
    private float lastX2 = Float.MIN_VALUE;
    private float lastY2 = Float.MIN_VALUE;
    private boolean centered;
    public float centeringZoom;

    public MapComponent(Context context, ArrayList<String> mapUrls) {
        super(context);
        tileLoader = new TileLoader(context, this, mapUrls);

        setZOrderOnTop(true); // no black flash on load

        setEGLContextClientVersion(2);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setCurrentPosition(Location location) {
        if (location != null) {
            double x = MercatorUtils.lonToMercatorX(location.getLongitude());
            double y = MercatorUtils.latToMercatorY(location.getLatitude());
            doSetCurrentPosition(new CurrentPosition(x, y, location.getBearing()));

            if (centered) {
                doSetPosition(x, y, centeringZoom);
            }
        } else {
            doSetCurrentPosition(null);
        }
    }

    public void setStartPosition(double lat, double lon) {
        if (lat == Double.MIN_VALUE) {
            doSetStartPosition(null);
        } else {
            doSetStartPosition(new Position(MercatorUtils.lonToMercatorX(lon), MercatorUtils.latToMercatorY(lat)));
        }
    }

    public void setEndPosition(double lat, double lon) {
        if (lat == Double.MIN_VALUE) {
            doSetEndPosition(null);
        } else {
            doSetEndPosition(new Position(MercatorUtils.lonToMercatorX(lon), MercatorUtils.latToMercatorY(lat)));
        }
    }

    public void moveTo(double lat, double lon, float zoom) {
        doSetPosition(MercatorUtils.lonToMercatorX(lon), MercatorUtils.latToMercatorY(lat), zoom);
    }

    public void startCentering() {
        centered = true;
        doCenterPosition();
    }

    public void queueEventOnDraw(Runnable runnable) {
        onDrawRunnables.add(runnable);
        requestRender();
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
            float preX1 = lastX1;
            float preY1 = lastY1;
            float preX2 = lastX2;
            float preY2 = lastY2;

            int pointerCount = event.getPointerCount();
            for (int i = 0; i < pointerCount; i++) {
                int id = event.getPointerId(i);
                float x = event.getX(i);
                float y = event.getY(i);

                if (id == 0) {
                    if (lastX2 == Float.MIN_VALUE) {
                        centered = false;
                        doMoveSingle(lastX1, lastY1, x, y);
                    }

                    lastX1 = x;
                    lastY1 = y;
                } else if (id == 1) {
                    lastX2 = x;
                    lastY2 = y;
                }
            }

            if (lastX1 != Float.MIN_VALUE && lastX2 != Float.MIN_VALUE) {
                centered = false;
                doMoveDouble(preX1, preY1, preX2, preY2, lastX1, lastY1, lastX2, lastY2);
            }
        }

        return true;
    }

    private void doMoveSingle(final float preX, final float preY, final float postX, final float postY) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.moveSingle(preX, preY, postX, postY);
            }
        });
    }

    private void doMoveDouble(final float preX1, final float preY1, final float preX2, final float preY2,
                              final float postX1, final float postY1, final float postX2, final float postY2) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.moveDouble(preX1, preY1, preX2, preY2, postX1, postY1, postX2, postY2);
            }
        });
    }

    private void doSetPosition(final double x, final double y, final float zoom) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setPosition(x, y, zoom);
            }
        });
    }

    private void doCenterPosition() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (renderer.currentPosition != null) {
                    renderer.setPosition(renderer.currentPosition.x, renderer.currentPosition.y, centeringZoom);
                }
            }
        });
    }

    private void doSetCurrentPosition(final CurrentPosition position) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setCurrentPosition(position);
            }
        });
    }

    private void doSetStartPosition(final Position startPosition) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setStartPosition(startPosition);
            }
        });
    }

    private void doSetEndPosition(final Position endPosition) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setEndPosition(endPosition);
            }
        });
    }

    public void close() {
        tileLoader.shutdownNow();
    }
}
