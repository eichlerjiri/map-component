package eichlerjiri.mapcomponent;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import eichlerjiri.mapcomponent.tiles.TileDownloadPool;
import eichlerjiri.mapcomponent.tiles.TileLoadPool;

import static eichlerjiri.mapcomponent.utils.Common.*;

public abstract class MapComponent extends RelativeLayout {

    public final GLSurfaceView glView;
    public final MapComponentRenderer renderer = new MapComponentRenderer(this);

    private final MapData d = new MapData();
    public volatile MapData dCommited = new MapData();

    public final TileLoadPool tileLoadPool;
    public final TileDownloadPool tileDownloadPool;

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
        tileLoadPool = new TileLoadPool(context, this);
        tileDownloadPool = new TileDownloadPool(this, mapUrls);

        glView.setZOrderOnTop(true); // no black flash on load
        glView.setZOrderMediaOverlay(true);

        centerButtonLayout = new LinearLayout(context);
        Button centerButton = new Button(context);
        centerButton.setBackgroundResource(R.mipmap.center);
        centerButtonLayout.addView(centerButton);
        centerButtonLayout.setBackgroundColor(0x99ffffff);

        float spSize = spSize(context);
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
                zoomIn(e.getX(), e.getY());
                commit();
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

        ret.putDouble("posX", d.posX);
        ret.putDouble("posY", d.posY);
        ret.putFloat("zoom", d.zoom);
        ret.putFloat("azimuth", d.azimuth);
        ret.putBoolean("centered", centered);

        return ret;
    }

    public void restoreInstanceState(final Bundle bundle) {
        d.posX = bundle.getDouble("posX");
        d.posY = bundle.getDouble("posY");
        d.zoom = bundle.getFloat("zoom");
        d.azimuth = bundle.getFloat("azimuth");
        commit();

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
                        moveSingle(lastX1, lastY1, x, y);
                        commit();
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
                moveDouble(preX1, preY1, preX2, preY2, lastX1, lastY1, lastX2, lastY2);
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

        float width = glView.getWidth();
        float height = glView.getHeight();

        float pixPadding = padding * 2 * spSize(getContext());
        if (width - pixPadding > 0) {
            width -= pixPadding;
        }
        if (height - pixPadding > 0) {
            height -= pixPadding;
        }

        double log2 = 1 / Math.log(2);
        double zoomX = Math.log(width / (renderer.tileSize * diffX)) * log2;
        double zoomY = Math.log(height / (renderer.tileSize * diffY)) * log2;

        float zoom = (float) Math.min(zoomX, zoomY);
        if (zoom != zoom || zoom == Float.NEGATIVE_INFINITY || zoom == Float.POSITIVE_INFINITY) {
            zoom = defaultZoom;
        }

        setZoom(zoom);
        setAzimuth(0);
    }

    public void moveSingle(float preX, float preY, float postX, float postY) {
        double mercatorPixelSize = mercatorPixelSize(renderer.tileSize, d.zoom);
        double x = (preX - postX) * mercatorPixelSize;
        double y = (preY - postY) * mercatorPixelSize;

        double rad = Math.toRadians(d.azimuth);
        double azimuthCos = Math.cos(rad);
        double azimuthSin = Math.sin(rad);

        setPosition(d.posX + x * azimuthCos + y * azimuthSin, d.posY + y * azimuthCos - x * azimuthSin);
    }

    public void moveDouble(float preX1, float preY1, float preX2, float preY2,
                           float postX1, float postY1, float postX2, float postY2) {
        double preMercatorPixelSize = mercatorPixelSize(renderer.tileSize, d.zoom);

        float preDist = computeDistance(preX1, preY1, preX2, preY2);
        float postDist = computeDistance(postX1, postY1, postX2, postY2);
        float diff = postDist / preDist;
        if (diff != diff) { // NaN result
            return;
        }

        setZoom(d.zoom + (float) (Math.log(diff) / Math.log(2)));

        double mercatorPixelSize = mercatorPixelSize(renderer.tileSize, d.zoom);

        float surfaceCenterX = glView.getWidth() * 0.5f;
        float surfaceCenterY = glView.getHeight() * 0.5f;
        preX1 -= surfaceCenterX;
        preY1 -= surfaceCenterY;
        preX2 -= surfaceCenterX;
        preY2 -= surfaceCenterY;
        postX1 -= surfaceCenterX;
        postY1 -= surfaceCenterY;
        postX2 -= surfaceCenterX;
        postY2 -= surfaceCenterY;

        double rad = Math.toRadians(d.azimuth);
        double azimuthCos = Math.cos(rad);
        double azimuthSin = Math.sin(rad);

        double posX1 = d.posX + (preX1 * azimuthCos + preY1 * azimuthSin) * preMercatorPixelSize;
        double posY1 = d.posY + (preY1 * azimuthCos - preX1 * azimuthSin) * preMercatorPixelSize;

        double lastAngle = Math.atan2(preX2 - preX1, preY2 - preY1);
        double angle = Math.atan2(postX2 - postX1, postY2 - postY1);
        setAzimuth(d.azimuth + (float) Math.toDegrees(lastAngle - angle));

        double posX2 = posX1 - (postX1 * azimuthCos + postY1 * azimuthSin) * mercatorPixelSize;
        double posY2 = posY1 - (postY1 * azimuthCos - postX1 * azimuthSin) * mercatorPixelSize;
        setPosition(posX2, posY2);
    }

    public void zoomIn(float x, float y) {
        double preMercatorPixelSize = mercatorPixelSize(renderer.tileSize, d.zoom);

        setZoom(Math.round(d.zoom + 1));

        double mercatorPixelSize = mercatorPixelSize(renderer.tileSize, d.zoom);

        x -= glView.getWidth() * 0.5f;
        y -= glView.getHeight() * 0.5f;

        double rad = Math.toRadians(d.azimuth);
        double azimuthCos = Math.cos(rad);
        double azimuthSin = Math.sin(rad);

        double px = d.posX + (x * azimuthCos + y * azimuthSin) * (preMercatorPixelSize - mercatorPixelSize);
        double py = d.posY + (y * azimuthCos - x * azimuthSin) * (preMercatorPixelSize - mercatorPixelSize);
        setPosition(px, py);
    }

    public void close() {
        tileLoadPool.shutdownNow();
        tileDownloadPool.shutdownNow();
    }
}
