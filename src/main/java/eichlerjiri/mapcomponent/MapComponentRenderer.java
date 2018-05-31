package eichlerjiri.mapcomponent;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Iterator;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import eichlerjiri.mapcomponent.shaders.ColorShader;
import eichlerjiri.mapcomponent.shaders.MapShader;
import eichlerjiri.mapcomponent.utils.CurrentPosition;
import eichlerjiri.mapcomponent.utils.FloatArrayList;
import eichlerjiri.mapcomponent.utils.GLUtils;
import eichlerjiri.mapcomponent.utils.MapTileKey;
import eichlerjiri.mapcomponent.utils.Position;

import static android.opengl.GLES20.*;

public class MapComponentRenderer implements GLSurfaceView.Renderer {

    public static final float tileSize = 256.0f;

    private final MapComponent mapComponent;
    private final float scaledDensity;

    private double posX = 0.5;
    private double posY = 0.5;
    private float zoom;

    public CurrentPosition currentPosition;
    public Position startPosition;
    public Position endPosition;
    public double[] path;

    private final HashMap<MapTileKey, TileCacheItem> tileCache = new HashMap<>();
    private final MapTileKey testKey = new MapTileKey();

    private MapShader mapShader;
    private ColorShader colorShader;
    private int mapVbuffer;
    private int mapVbufferCount;
    private int currentPositionVbuffer;
    private int currentPositionVbufferCount;
    private int positionVbuffer;
    private int positionVbufferCount;
    private int pathVbuffer;

    private int w;
    private int h;

    // zoom-related values
    private int mapZoom;
    private int tiles;
    private double tilesReversed;
    private double mercatorPixels;
    private double mercatorPixelSize;
    private float scale;

    // screen-size related values
    private float surfaceCenterX;
    private float surfaceCenterY;
    private int searchDist;
    private final float[] mapMatrix = new float[16];

    // tmps
    private final int[] itmp1 = new int[1];
    private final FloatArrayList fltmp1 = new FloatArrayList();
    private final float[] tmpMatrix = new float[16];

    private int tick;

    public MapComponentRenderer(MapComponent mapComponent) {
        this.mapComponent = mapComponent;
        scaledDensity = mapComponent.getContext().getResources().getDisplayMetrics().scaledDensity;

        refreshScreenSizeValues();
        refreshZoomValues();
    }

    public void setPosition(double x, double y, float newZoom) {
        setZoom(newZoom);
        setPos(x, y);
        mapComponent.requestRender();
    }

    public void setCurrentPosition(CurrentPosition currentPosition) {
        this.currentPosition = currentPosition;
        mapComponent.requestRender();
    }

    public void setStartPosition(Position startPosition) {
        this.startPosition = startPosition;
        mapComponent.requestRender();
    }

    public void setEndPosition(Position endPosition) {
        this.endPosition = endPosition;
        mapComponent.requestRender();
    }

    public void setPath(double[] path) {
        this.path = path;
        mapComponent.requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.8f, 0.97f, 1.0f, 1.0f);

        tileCache.clear();

        mapShader = new MapShader();

        float[] mapBufferData = new float[]{0, 0, 0, 1, 1, 1, 1, 0};
        mapVbuffer = GLUtils.prepareStaticBuffer(mapBufferData, itmp1);
        mapVbufferCount = mapBufferData.length / 2;

        colorShader = null;
        currentPositionVbuffer = 0;
        positionVbuffer = 0;
        pathVbuffer = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);

        w = width;
        h = height;

        refreshScreenSizeValues();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        while (true) {
            Runnable r = mapComponent.onDrawRunnables.poll();
            if (r == null) {
                break;
            }
            r.run();
        }

        tick++;
        glClear(GL_COLOR_BUFFER_BIT);

        double centerX = tiles * posX;
        double centerY = tiles * posY;

        int centerTileX = (int) centerX;
        int centerTileY = (int) centerY;
        if (centerTileY == tiles) {
            centerTileY--;
        }

        for (int i = centerTileX - searchDist; i <= centerTileX + searchDist; i++) {
            for (int j = centerTileY - searchDist; j <= centerTileY + searchDist; j++) {
                drawTile(i, j);
            }
        }

        if (path != null) {
            if (pathVbuffer == 0) {
                glGenBuffers(1, itmp1, 0);
                pathVbuffer = itmp1[0];
            }
            drawPath();
        } else if (pathVbuffer != 0) {
            itmp1[0] = pathVbuffer;
            glDeleteBuffers(1, itmp1, 0);
            pathVbuffer = 0;
        }
        if (startPosition != null) {
            drawPosition(startPosition, 0, 1, 0, 1);
        }
        if (endPosition != null) {
            drawPosition(endPosition, 1, 0, 0, 1);
        }
        if (currentPosition != null) {
            drawCurrentPosition();
        }

        mapComponent.tileLoader.cancelUnused(tick);
        removeUnused();
    }

    private void drawTile(int tileX, int tileY) {
        if (tileY < 0 || tileY >= tiles) {
            return;
        }

        int tileXreal = tileX;
        while (tileXreal < 0) {
            tileXreal += tiles;
        }
        while (tileXreal >= tiles) {
            tileXreal -= tiles;
        }

        testKey.zoom = mapZoom;
        testKey.x = tileXreal;
        testKey.y = tileY;
        TileCacheItem cacheItem = tileCache.get(testKey);

        if (cacheItem == null) {
            mapComponent.tileLoader.requestTile(testKey, tick);
            return;
        }

        cacheItem.tick = tick;
        if (cacheItem.textureID == 0) {
            return;
        }

        float translateX = translateX(tileX * tilesReversed);
        float translateY = translateY(tileY * tilesReversed);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, translateX, translateY, 0);
        Matrix.scaleM(tmpMatrix, 0, scale, scale, 1);

        mapShader.render(tmpMatrix, mapVbuffer, mapVbufferCount, cacheItem.textureID, GL_TRIANGLE_FAN);
    }

    private void drawCurrentPosition() {
        if (colorShader == null) {
            colorShader = new ColorShader();
        }
        if (currentPositionVbuffer == 0) {
            float[] data = new float[]{-10, 10, 0, -3, 0, -10, 10, 10, 0, -3, 0, -10};
            for (int i = 0; i < data.length; i++) {
                data[i] *= scaledDensity;
            }
            currentPositionVbuffer = GLUtils.prepareStaticBuffer(data, itmp1);
            currentPositionVbufferCount = data.length / 2;
        }

        float translateX = translateX(currentPosition.x);
        float translateY = translateY(currentPosition.y);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, translateX, translateY, 0);
        Matrix.rotateM(tmpMatrix, 0, currentPosition.azimuth, 0, 0, 1);

        colorShader.render(tmpMatrix, currentPositionVbuffer, currentPositionVbufferCount, GL_TRIANGLES, 0, 0, 1, 1);
    }

    private void drawPosition(Position position, float r, float g, float b, float a) {
        if (colorShader == null) {
            colorShader = new ColorShader();
        }
        if (positionVbuffer == 0) {
            float[] data = new float[]{0, 0, 10, -20, -10, -20};
            for (int i = 0; i < data.length; i++) {
                data[i] *= scaledDensity;
            }
            positionVbuffer = GLUtils.prepareStaticBuffer(data, itmp1);
            positionVbufferCount = data.length / 2;
        }

        float translateX = translateX(position.x);
        float translateY = translateY(position.y);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, translateX, translateY, 0);

        colorShader.render(tmpMatrix, positionVbuffer, positionVbufferCount, GL_TRIANGLES, r, g, b, a);
    }

    private void drawPath() {
        if (colorShader == null) {
            colorShader = new ColorShader();
        }

        for (int i = 2; i < path.length; i += 2) {
            float tx1 = translateX(path[i - 2]);
            float ty1 = translateY(path[i - 1]);
            float tx2 = translateX(path[i]);
            float ty2 = translateY(path[i + 1]);

            float vx = tx2 - tx1;
            float vy = ty2 - ty1;

            float norm = 1 / (float) Math.sqrt(vx * vx + vy * vy);
            vx *= norm;
            vy *= norm;

            float tx11 = tx1 + vy * 3;
            float ty11 = ty1 - vx * 3;
            float tx12 = tx1 - vy * 3;
            float ty12 = ty1 + vx * 3;
            float tx21 = tx2 + vy * 3;
            float ty21 = ty2 - vx * 3;
            float tx22 = tx2 - vy * 3;
            float ty22 = ty2 + vx * 3;

            fltmp1.add(tx11, ty11, tx12, ty12, tx21, ty21, tx12, ty12, tx21, ty21, tx22, ty22);
        }

        if (fltmp1.size == 0) {
            return;
        }

        glBindBuffer(GL_ARRAY_BUFFER, pathVbuffer);
        glBufferData(GL_ARRAY_BUFFER, fltmp1.size * 4, FloatBuffer.wrap(fltmp1.data, 0, fltmp1.size), GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        colorShader.render(mapMatrix, pathVbuffer, fltmp1.size / 2, GL_TRIANGLES, 0, 0, 0, 1);

        fltmp1.clear();
    }

    public void moveSingle(float preX, float preY, float postX, float postY) {
        double x = posX + (preX - postX) * mercatorPixelSize;
        double y = posY + (preY - postY) * mercatorPixelSize;
        setPos(x, y);

        mapComponent.requestRender();
    }

    private float translateX(double mercatorX) {
        return surfaceCenterX + (float) ((mercatorX - posX) * mercatorPixels);
    }

    private float translateY(double mercatorY) {
        return surfaceCenterY + (float) ((mercatorY - posY) * mercatorPixels);
    }

    public void moveDouble(float preX1, float preY1, float preX2, float preY2,
                           float postX1, float postY1, float postX2, float postY2) {
        double preMercatorPixelSize = mercatorPixelSize;

        float preDist = computeDistance(preX1, preY1, preX2, preY2);
        float postDist = computeDistance(postX1, postY1, postX2, postY2);
        float diff = postDist / preDist;
        if (diff != diff) { // NaN result
            return;
        }

        setZoom(zoom + (float) (Math.log(diff) / Math.log(2)));

        double posX1 = posX + (preX1 - surfaceCenterX) * preMercatorPixelSize;
        double posY1 = posY + (preY1 - surfaceCenterY) * preMercatorPixelSize;

        double posX2 = posX1 - (postX1 - surfaceCenterX) * mercatorPixelSize;
        double posY2 = posY1 - (postY1 - surfaceCenterY) * mercatorPixelSize;
        setPos(posX2, posY2);

        mapComponent.requestRender();
    }

    private void setPos(double x, double y) {
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

        posX = x;
        posY = y;
    }

    private void setZoom(float newZoom) {
        if (newZoom < 0) {
            newZoom = 0;
        } else if (newZoom > 19) {
            newZoom = 19;
        }

        zoom = newZoom;
        refreshZoomValues();
    }

    private float computeDistance(float x1, float y1, float x2, float y2) {
        float xDiff = x1 - x2;
        float yDiff = y1 - y2;
        return (float) Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    public void tileLoaded(MapTileKey key, int width, int height, ByteBuffer data) {
        if (data == null) {
            tileCache.put(key, new TileCacheItem(0, tick));
            return;
        }

        glGenTextures(1, itmp1, 0);
        int textureId = itmp1[0];

        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);

        tileCache.put(key, new TileCacheItem(textureId, tick));
        mapComponent.requestRender();
    }

    private void removeUnused() {
        Iterator<TileCacheItem> it = tileCache.values().iterator();
        while (it.hasNext()) {
            TileCacheItem item = it.next();
            if (item.tick != tick) {
                if (item.textureID != 0) {
                    itmp1[0] = item.textureID;
                    glDeleteTextures(1, itmp1, 0);
                }
                it.remove();
            }
        }
    }

    private void refreshScreenSizeValues() {
        surfaceCenterX = w * 0.5f;
        surfaceCenterY = h * 0.5f;

        int tilesNecessaryBase = (int) (Math.sqrt(w * w + h * h) / tileSize);
        searchDist = (tilesNecessaryBase + 1) / 2;

        if (w != 0 & h != 0) {
            Matrix.orthoM(mapMatrix, 0, 0, w, h, 0, -10, 10);
        } else {
            Matrix.setIdentityM(mapMatrix, 0);
        }
    }

    private void refreshZoomValues() {
        mapZoom = (int) zoom;
        tiles = 1 << mapZoom;
        tilesReversed = 1 / (double) tiles;
        mercatorPixels = tileSize * Math.pow(2, zoom);
        mercatorPixelSize = 1 / mercatorPixels;
        scale = (float) (mercatorPixels * tilesReversed);
    }

    private class TileCacheItem {

        public final int textureID;
        public int tick;

        public TileCacheItem(int textureID, int tick) {
            this.textureID = textureID;
            this.tick = tick;
        }
    }
}
