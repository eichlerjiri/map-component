package eichlerjiri.mapcomponent;

import android.content.Context;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import eichlerjiri.mapcomponent.utils.AndroidUtils;
import eichlerjiri.mapcomponent.utils.CurrentPosition;
import eichlerjiri.mapcomponent.utils.DoubleArrayList;
import eichlerjiri.mapcomponent.utils.GeoBoundary;
import eichlerjiri.mapcomponent.utils.MercatorUtils;
import eichlerjiri.mapcomponent.utils.Position;

public abstract class MapComponent extends RelativeLayout {

    public final GLSurfaceView glView;
    public final MapComponentRenderer renderer = new MapComponentRenderer(this);
    public final TileLoader tileLoader;
    public final TileDownloader tileDownloader;

    public final ConcurrentLinkedQueue<Runnable> onDrawRunnables = new ConcurrentLinkedQueue<>();
    private final GestureDetector gestureDetector;

    private final LinearLayout centerButtonLayout;

    private float lastX1 = Float.MIN_VALUE;
    private float lastY1 = Float.MIN_VALUE;
    private float lastX2 = Float.MIN_VALUE;
    private float lastY2 = Float.MIN_VALUE;

    public boolean centered = true;

    public MapComponent(Context context, ArrayList<String> mapUrls) {
        super(context);
        glView = new GLSurfaceView(context);
        tileLoader = new TileLoader(context, this);
        tileDownloader = new TileDownloader(this, mapUrls);

        glView.setZOrderOnTop(true); // no black flash on load
        glView.setZOrderMediaOverlay(true);

        centerButtonLayout = new LinearLayout(context);
        Button centerButton = new Button(context);
        centerButton.setBackgroundResource(R.mipmap.center);
        centerButtonLayout.addView(centerButton);
        centerButtonLayout.setBackgroundColor(0x99ffffff);

        float spSize = AndroidUtils.spSize(context);
        int size = Math.round(40 * spSize);
        int margin = Math.round(10 * spSize);

        LayoutParams params = new LayoutParams(size, size);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.setMargins(margin, margin, margin, margin);
        centerButtonLayout.setLayoutParams(params);

        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        addView(glView);

        gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                doZoomIn(e.getX(), e.getY());
                return true;
            }
        });

        centerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startCentering();
            }
        });
    }

    public Bundle saveInstanceState() {
        Bundle ret = new Bundle();

        ret.putDouble("posX", renderer.posX);
        ret.putDouble("posY", renderer.posY);
        ret.putFloat("zoom", renderer.zoom);
        ret.putFloat("azimuth", renderer.azimuth);
        ret.putBoolean("centered", centered);

        return ret;
    }

    public void restoreInstanceState(final Bundle bundle) {
        doSetPosition(bundle.getDouble("posX"), bundle.getDouble("posY"), bundle.getFloat("zoom"),
                bundle.getFloat("azimuth"));

        if (bundle.getBoolean("centered")) {
            startCentering();
        } else {
            stopCentering();
        }
    }

    public abstract void centerMap();

    public void stopCentering() {
        if (centered) {
            addView(centerButtonLayout);
            centered = false;
        }
    }

    public void startCentering() {
        if (!centered) {
            removeView(centerButtonLayout);
            centered = true;
            centerMap();
        }
    }

    public void setCurrentPosition(Location location) {
        if (location != null) {
            double x = MercatorUtils.lonToMercatorX(location.getLongitude());
            double y = MercatorUtils.latToMercatorY(location.getLatitude());
            float bearing = location.hasBearing() ? location.getBearing() : Float.MIN_VALUE;
            doSetCurrentPosition(new CurrentPosition(x, y, bearing));
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

    public void setPath(DoubleArrayList positions) {
        if (positions == null) {
            doSetPath(null);
        } else {
            double[] mercatorPos = new double[positions.size];
            for (int i = 0; i < positions.size; i += 2) {
                mercatorPos[i] = MercatorUtils.lonToMercatorX(positions.data[i + 1]);
                mercatorPos[i + 1] = MercatorUtils.latToMercatorY(positions.data[i]);
            }
            doSetPath(mercatorPos);
        }
    }

    public void moveTo(double lat, double lon, float zoom, float azimuth) {
        doSetPosition(MercatorUtils.lonToMercatorX(lon), MercatorUtils.latToMercatorY(lat), zoom, azimuth);
    }

    public void moveToBoundary(GeoBoundary geoBoundary, float viewWidth, float viewHeight,
                               float defaultZoom, float padding) {
        double x1 = MercatorUtils.lonToMercatorX(geoBoundary.minLon);
        double y1 = MercatorUtils.latToMercatorY(geoBoundary.minLat);
        double x2 = MercatorUtils.lonToMercatorX(geoBoundary.maxLon);
        double y2 = MercatorUtils.latToMercatorY(geoBoundary.maxLat);

        double diffX = x2 - x1;
        double diffY = y1 - y2;

        float pixPadding = padding * 2 * AndroidUtils.spSize(getContext());
        if (viewWidth - pixPadding > 0) {
            viewWidth -= pixPadding;
        }
        if (viewHeight - pixPadding > 0) {
            viewHeight -= pixPadding;
        }

        double log2 = 1 / Math.log(2);
        double zoomX = Math.log(viewWidth / (renderer.tileSize * diffX)) * log2;
        double zoomY = Math.log(viewHeight / (renderer.tileSize * diffY)) * log2;

        float zoom = (float) Math.min(zoomX, zoomY);
        if (zoom != zoom || zoom == Float.NEGATIVE_INFINITY || zoom == Float.POSITIVE_INFINITY) {
            zoom = defaultZoom;
        }

        doSetPosition((x1 + x2) * 0.5, (y1 + y2) * 0.5, zoom, 0);
    }

    public void queueEventOnDraw(Runnable runnable) {
        onDrawRunnables.add(runnable);
        glView.requestRender();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
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
                        stopCentering();
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
                stopCentering();
                doMoveDouble(preX1, preY1, preX2, preY2, lastX1, lastY1, lastX2, lastY2);
            }
        }

        return true;
    }

    private void doMoveSingle(final float preX, final float preY, final float postX, final float postY) {
        glView.queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.moveSingle(preX, preY, postX, postY);
            }
        });
    }

    private void doMoveDouble(final float preX1, final float preY1, final float preX2, final float preY2,
                              final float postX1, final float postY1, final float postX2, final float postY2) {
        glView.queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.moveDouble(preX1, preY1, preX2, preY2, postX1, postY1, postX2, postY2);
            }
        });
    }

    private void doZoomIn(final float x, final float y) {
        glView.queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.zoomIn(x, y);
            }
        });
    }

    private void doSetPosition(final double x, final double y, final float zoom, final float azimuth) {
        glView.queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setPosition(x, y, zoom, azimuth);
            }
        });
    }

    private void doSetCurrentPosition(final CurrentPosition position) {
        glView.queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setCurrentPosition(position);
            }
        });
    }

    private void doSetStartPosition(final Position startPosition) {
        glView.queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setStartPosition(startPosition);
            }
        });
    }

    private void doSetEndPosition(final Position endPosition) {
        glView.queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setEndPosition(endPosition);
            }
        });
    }

    private void doSetPath(final double[] path) {
        glView.queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setPath(path);
            }
        });
    }

    public void close() {
        tileLoader.shutdownNow();
        tileDownloader.shutdownNow();
    }
}
