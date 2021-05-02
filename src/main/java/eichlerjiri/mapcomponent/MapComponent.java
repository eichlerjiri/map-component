package eichlerjiri.mapcomponent;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import eichlerjiri.mapcomponent.tiles.TileDownloadPool;
import eichlerjiri.mapcomponent.tiles.TileLoadPool;
import eichlerjiri.mapcomponent.utils.ObjectList;

import static eichlerjiri.mapcomponent.utils.Common.*;
import static java.lang.Math.*;

public abstract class MapComponent extends GLSurfaceView {

    public final float spSize;
    public final float tileSize;

    public MapComponentRenderer renderer;
    public final MapData d = new MapData();
    public volatile MapData dCommited = new MapData();

    public final TileLoadPool tileLoadPool;
    public final TileDownloadPool tileDownloadPool;

    public final GestureDetector gestureDetector;

    public int pressed = -1;
    public float lastX1 = Float.NEGATIVE_INFINITY;
    public float lastY1 = Float.NEGATIVE_INFINITY;
    public float lastX2 = Float.NEGATIVE_INFINITY;
    public float lastY2 = Float.NEGATIVE_INFINITY;

    public MapComponent(Context context, ObjectList<String> mapUrls) {
        super(context);
        spSize = context.getResources().getDisplayMetrics().scaledDensity;
        tileSize = 256 * spSize;

        tileLoadPool = new TileLoadPool(context, this);
        tileDownloadPool = new TileDownloadPool(mapUrls);

        setZOrderOnTop(true); // no black flash on load
        setZOrderMediaOverlay(true);

        setEGLContextClientVersion(2);
        setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                renderer = new MapComponentRenderer(MapComponent.this);
            }

            @Override
            public void onSurfaceChanged(GL10 gl10, int i, int i1) {
                renderer.setDimensions(i, i1);
            }

            @Override
            public void onDrawFrame(GL10 gl10) {
                renderer.drawFrame();
            }
        });
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                zz_zoomIn(e.getX(), e.getY());
                commit();
                return true;
            }
        });
    }

    public Bundle saveInstanceState() {
        Bundle ret = new Bundle();

        ret.putDouble("posX", d.posX);
        ret.putDouble("posY", d.posY);
        ret.putFloat("zoom", d.zoom);
        ret.putFloat("azimuth", d.azimuth);
        ret.putBoolean("centered", d.centered);

        return ret;
    }

    public void restoreInstanceState(Bundle bundle) {
        d.posX = bundle.getDouble("posX");
        d.posY = bundle.getDouble("posY");
        d.zoom = bundle.getFloat("zoom");
        d.azimuth = bundle.getFloat("azimuth");

        if (bundle.getBoolean("centered")) {
            zz_startCentering();
        } else {
            zz_stopCentering();
        }

        commit();
    }

    public abstract void centerMap();

    public void setCurrentPosition(double x, double y, float azimuth) {
        d.currentPositionX = x;
        d.currentPositionY = y;
        d.currentPositionAzimuth = azimuth;
    }

    public void setStartPosition(double x, double y) {
        d.startPositionX = x;
        d.startPositionY = y;
    }

    public void setEndPosition(double x, double y) {
        d.endPositionX = x;
        d.endPositionY = y;
    }

    public void setPath(double[] path, int offset, int length) {
        d.path = path;
        d.pathOffset = offset;
        d.pathLength = length;
    }

    public void commit() {
        dCommited = d.copy();
        requestRender();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        int action = event.getActionMasked();

        if (pressed != -1) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                if (pressed == event.getPointerId(event.getActionIndex())) {
                    pressed = -1;
                }
            }
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int index = event.getActionIndex();
            int id = event.getPointerId(index);
            float x = event.getX(index);
            float y = event.getY(index);

            if (id == 0) {
                MapDataRenderer dr = renderer.dr;
                if (dr.centerButtonX <= x && dr.centerButtonX + dr.centerButtonWidth > x && dr.centerButtonY <= y && dr.centerButtonY + dr.centerButtonHeight > y) {
                    pressed = id;
                    zz_startCentering();
                    commit();
                } else {
                    lastX1 = x;
                    lastY1 = y;
                }
            } else if (id == 1) {
                lastX2 = x;
                lastY2 = y;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            int id = event.getPointerId(event.getActionIndex());

            if (id == 0) {
                lastX1 = Float.NEGATIVE_INFINITY;
                lastY1 = Float.NEGATIVE_INFINITY;
            } else if (id == 1) {
                lastX2 = Float.NEGATIVE_INFINITY;
                lastY2 = Float.NEGATIVE_INFINITY;
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
                    if (lastX1 != Float.NEGATIVE_INFINITY && lastX2 == Float.NEGATIVE_INFINITY) {
                        zz_stopCentering();
                        zz_moveSingle(lastX1, lastY1, x, y);
                        commit();
                    }

                    lastX1 = x;
                    lastY1 = y;
                } else if (id == 1) {
                    lastX2 = x;
                    lastY2 = y;
                }
            }

            if (lastX1 != Float.NEGATIVE_INFINITY && lastX2 != Float.NEGATIVE_INFINITY) {
                zz_stopCentering();
                zz_moveDouble(preX1, preY1, preX2, preY2, lastX1, lastY1, lastX2, lastY2);
                commit();
            }
        }

        return true;
    }

    public void setPosition(double x, double y) {
        while (x < 0) {
            x++;
        }
        while (x > 1) {
            x--;
        }

        if (y < 0) {
            y = 0;
        } else if (y > 1) {
            y = 1;
        }

        d.posX = x;
        d.posY = y;
    }

    public void setZoom(float zoom) {
        if (zoom < 0) {
            zoom = 0;
        } else if (zoom > 20) {
            zoom = 20;
        }

        d.zoom = zoom;
    }

    public void setAzimuth(float azimuth) {
        while (azimuth < 0) {
            azimuth += 360;
        }
        while (azimuth > 360) {
            azimuth -= 360;
        }

        d.azimuth = azimuth;
    }

    public void moveToBoundary(double x1, double y1, double x2, double y2, float defaultZoom, float padding) {
        setPosition((x1 + x2) * 0.5, (y1 + y2) * 0.5);

        double diffX = x2 - x1;
        double diffY = y2 - y1;

        float width = getWidth();
        float height = getHeight();

        float pixPadding = padding * 2 * spSize;
        if (width - pixPadding > 0) {
            width -= pixPadding;
        }
        if (height - pixPadding > 0) {
            height -= pixPadding;
        }

        double log2 = 1 / log(2);
        double zoomX = log(width / (tileSize * diffX)) * log2;
        double zoomY = log(height / (tileSize * diffY)) * log2;

        float zoom = (float) min(zoomX, zoomY);
        if (zoom != zoom || zoom == Float.NEGATIVE_INFINITY || zoom == Float.POSITIVE_INFINITY) {
            zoom = defaultZoom;
        }

        setZoom(zoom);
        setAzimuth(0);
    }

    public void close() {
        tileLoadPool.shutdownNow();
        tileDownloadPool.shutdownNow();
    }

    public void zz_stopCentering() {
        d.centered = false;
    }

    public void zz_startCentering() {
        if (!d.centered) {
            d.centered = true;
            centerMap();
        }
    }

    public void zz_moveSingle(float preX, float preY, float postX, float postY) {
        double mercatorPixelSize = 1 / (tileSize * pow(2, d.zoom));
        double x = (preX - postX) * mercatorPixelSize;
        double y = (preY - postY) * mercatorPixelSize;

        double rad = toRadians(d.azimuth);
        double azimuthCos = cos(rad);
        double azimuthSin = sin(rad);

        setPosition(d.posX + x * azimuthCos + y * azimuthSin, d.posY + y * azimuthCos - x * azimuthSin);
    }

    public void zz_moveDouble(float preX1, float preY1, float preX2, float preY2,
            float postX1, float postY1, float postX2, float postY2) {
        float preDist = computeDistance(preX1, preY1, preX2, preY2);
        float postDist = computeDistance(postX1, postY1, postX2, postY2);
        float diff = postDist / preDist;
        if (diff != diff) { // NaN result
            return;
        }

        double preMercatorPixelSize = 1 / (tileSize * pow(2, d.zoom));

        setZoom(d.zoom + (float) (log(diff) / log(2)));

        double mercatorPixelSize = 1 / (tileSize * pow(2, d.zoom));

        float surfaceCenterX = getWidth() * 0.5f;
        float surfaceCenterY = getHeight() * 0.5f;
        preX1 -= surfaceCenterX;
        preY1 -= surfaceCenterY;
        preX2 -= surfaceCenterX;
        preY2 -= surfaceCenterY;
        postX1 -= surfaceCenterX;
        postY1 -= surfaceCenterY;
        postX2 -= surfaceCenterX;
        postY2 -= surfaceCenterY;

        double rad = toRadians(d.azimuth);
        double azimuthCos = cos(rad);
        double azimuthSin = sin(rad);

        double posX1 = d.posX + (preX1 * azimuthCos + preY1 * azimuthSin) * preMercatorPixelSize;
        double posY1 = d.posY + (preY1 * azimuthCos - preX1 * azimuthSin) * preMercatorPixelSize;

        double lastAngle = atan2(preX2 - preX1, preY2 - preY1);
        double angle = atan2(postX2 - postX1, postY2 - postY1);
        setAzimuth(d.azimuth + (float) toDegrees(lastAngle - angle));

        double posX2 = posX1 - (postX1 * azimuthCos + postY1 * azimuthSin) * mercatorPixelSize;
        double posY2 = posY1 - (postY1 * azimuthCos - postX1 * azimuthSin) * mercatorPixelSize;
        setPosition(posX2, posY2);
    }

    public void zz_zoomIn(float x, float y) {
        double preMercatorPixelSize = 1 / (tileSize * pow(2, d.zoom));

        setZoom(round(d.zoom + 1));

        double mercatorPixelSize = 1 / (tileSize * pow(2, d.zoom));

        x -= getWidth() * 0.5f;
        y -= getHeight() * 0.5f;

        double rad = toRadians(d.azimuth);
        double azimuthCos = cos(rad);
        double azimuthSin = sin(rad);

        double px = d.posX + (x * azimuthCos + y * azimuthSin) * (preMercatorPixelSize - mercatorPixelSize);
        double py = d.posY + (y * azimuthCos - x * azimuthSin) * (preMercatorPixelSize - mercatorPixelSize);
        setPosition(px, py);
    }
}
