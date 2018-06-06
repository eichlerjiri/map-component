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
import eichlerjiri.mapcomponent.utils.AndroidUtils;
import eichlerjiri.mapcomponent.utils.CurrentPosition;
import eichlerjiri.mapcomponent.utils.FloatArrayList;
import eichlerjiri.mapcomponent.utils.GLUtils;
import eichlerjiri.mapcomponent.utils.MapTileKey;
import eichlerjiri.mapcomponent.utils.Position;

import static android.opengl.GLES20.*;

public class MapComponentRenderer implements GLSurfaceView.Renderer {

    private final MapComponent mapComponent;
    private final float spSize;
    public final float tileSize;

    private double posX = 0.5;
    private double posY = 0.5;
    private float zoom;
    private float azimuth;

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
    private int nodirCurrentPositionVbuffer;
    private int nodirCurrentPositionVbufferCount;
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

    // azimuth related values
    private float azimuthSin;
    private float azimuthCos;

    // tmps
    private final int[] itmp1 = new int[1];
    private final FloatArrayList fltmp1 = new FloatArrayList();
    private final float[] tmpMatrix = new float[16];
    private float ftmp1;
    private float ftmp2;

    private int tick;

    public MapComponentRenderer(MapComponent mapComponent) {
        this.mapComponent = mapComponent;
        spSize = AndroidUtils.spSize(mapComponent.getContext());
        tileSize = 256 * spSize;

        refreshScreenSizeValues();
        refreshZoomValues();
        refreshAzimuthValues();
    }

    public void setPosition(double x, double y, float newZoom, float azimuth) {
        setZoom(newZoom);
        setPos(x, y);
        setAzimuth(azimuth);
        mapComponent.glView.requestRender();
    }

    public void setCurrentPosition(CurrentPosition currentPosition) {
        this.currentPosition = currentPosition;
        mapComponent.glView.requestRender();
    }

    public void setStartPosition(Position startPosition) {
        this.startPosition = startPosition;
        mapComponent.glView.requestRender();
    }

    public void setEndPosition(Position endPosition) {
        this.endPosition = endPosition;
        mapComponent.glView.requestRender();
    }

    public void setPath(double[] path) {
        this.path = path;
        mapComponent.glView.requestRender();
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
        nodirCurrentPositionVbuffer = 0;
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
                drawTile(i, j, Math.abs(i - centerTileX) + Math.abs(j - centerTileY));
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

    private void drawTile(int tileXview, int tileY, int toCenter) {
        if (tileY < 0 || tileY >= tiles) {
            return;
        }

        int tileX = tileXview;
        while (tileX < 0) {
            tileX += tiles;
        }
        while (tileX >= tiles) {
            tileX -= tiles;
        }

        // regular tile, matching zoom
        int texture = getTexture(mapZoom, tileX, tileY, toCenter);
        if (texture != 0) {
            preparePoint(tileXview * tilesReversed, tileY * tilesReversed);
            Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmp1, ftmp2, 0);
            Matrix.scaleM(tmpMatrix, 0, scale, scale, 1);
            Matrix.rotateM(tmpMatrix, 0, azimuth, 0, 0, 1);

            mapShader.render(tmpMatrix, mapVbuffer, mapVbufferCount, texture, GL_TRIANGLE_FAN, 1, 1, 0, 0);
            return;
        }

        // lower zoom tile, going up to zoom 0
        int testZoom = mapZoom - 1;
        int testX = tileX / 2;
        int testY = tileY / 2;
        while (testZoom >= 0) {
            texture = getTexture(testZoom, testX, testY, (mapZoom - testZoom) * 1000000 + toCenter);
            if (texture != 0) {
                preparePoint(tileXview * tilesReversed, tileY * tilesReversed);
                Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmp1, ftmp2, 0);
                Matrix.scaleM(tmpMatrix, 0, scale, scale, 1);
                Matrix.rotateM(tmpMatrix, 0, azimuth, 0, 0, 1);

                int zoomDiffTiles = 1 << (mapZoom - testZoom);
                float scaleTile = 1.0f / zoomDiffTiles;
                float shiftX = scaleTile * (tileX - testX * zoomDiffTiles);
                float shiftY = scaleTile * (tileY - testY * zoomDiffTiles);

                mapShader.render(tmpMatrix, mapVbuffer, mapVbufferCount, texture, GL_TRIANGLE_FAN,
                        scaleTile, scaleTile, shiftX, shiftY);
                return;
            }

            testZoom--;
            testX /= 2;
            testY /= 2;
        }

        // try one higher zoom, for zooming out
        testZoom = mapZoom + 1;
        testX = tileX * 2;
        testY = tileY * 2;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                int curTileX = testX + i;
                int curTileY = testY + j;

                texture = getTexture(testZoom, curTileX, curTileY, 100000000 + toCenter);
                if (texture != 0) {
                    preparePoint(curTileX * tilesReversed * 0.5f, curTileY * tilesReversed * 0.5f);
                    Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmp1, ftmp2, 0);
                    Matrix.scaleM(tmpMatrix, 0, scale * 0.5f, scale * 0.5f, 1);
                    Matrix.rotateM(tmpMatrix, 0, azimuth, 0, 0, 1);

                    mapShader.render(tmpMatrix, mapVbuffer, mapVbufferCount, texture, GL_TRIANGLE_FAN, 1, 1, 0, 0);
                }
            }
        }
    }

    private int getTexture(int zoom, int x, int y, int priority) {
        if (zoom < 0 || zoom > 19) {
            return 0;
        }

        testKey.zoom = zoom;
        testKey.x = x;
        testKey.y = y;
        TileCacheItem cacheItem = tileCache.get(testKey);
        if (cacheItem == null) {
            mapComponent.tileLoader.requestTile(testKey, tick, priority);
            return 0;
        }

        cacheItem.tick = tick;
        return cacheItem.textureID;
    }

    private void drawCurrentPosition() {
        if (colorShader == null) {
            colorShader = new ColorShader();
        }

        preparePoint(currentPosition.x, currentPosition.y);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmp1, ftmp2, 0);

        if (currentPosition.azimuth != Float.MIN_VALUE) {
            if (currentPositionVbuffer == 0) {
                float[] data = new float[]{-12, 12, 0, -4, 0, -12, 12, 12, 0, -4, 0, -12};
                for (int i = 0; i < data.length; i++) {
                    data[i] *= spSize;
                }
                currentPositionVbuffer = GLUtils.prepareStaticBuffer(data, itmp1);
                currentPositionVbufferCount = data.length / 2;
            }

            Matrix.rotateM(tmpMatrix, 0, azimuth + currentPosition.azimuth, 0, 0, 1);

            colorShader.render(tmpMatrix, currentPositionVbuffer, currentPositionVbufferCount, GL_TRIANGLES,
                    0, 0, 1, 1);
        } else {
            if (nodirCurrentPositionVbuffer == 0) {
                float[] data = new float[]{
                        -12, 0, -8, 0, 0, 12, 0, 12, 0, 8, -8, 0,
                        12, 0, 8, 0, 0, -12, 0, -12, 0, -8, 8, 0,
                        -12, 0, -8, 0, 0, -12, 0, -12, 0, -8, -8, 0,
                        12, 0, 8, 0, 0, 12, 0, 12, 0, 8, 8, 0
                };
                for (int i = 0; i < data.length; i++) {
                    data[i] *= spSize;
                }
                nodirCurrentPositionVbuffer = GLUtils.prepareStaticBuffer(data, itmp1);
                nodirCurrentPositionVbufferCount = data.length / 2;
            }

            colorShader.render(tmpMatrix, nodirCurrentPositionVbuffer, nodirCurrentPositionVbufferCount, GL_TRIANGLES,
                    0, 0, 1, 1);
        }
    }

    private void drawPosition(Position position, float r, float g, float b, float a) {
        if (colorShader == null) {
            colorShader = new ColorShader();
        }
        if (positionVbuffer == 0) {
            float[] data = new float[]{0, 0, 10, -20, -10, -20};
            for (int i = 0; i < data.length; i++) {
                data[i] *= spSize;
            }
            positionVbuffer = GLUtils.prepareStaticBuffer(data, itmp1);
            positionVbufferCount = data.length / 2;
        }

        preparePoint(position.x, position.y);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmp1, ftmp2, 0);

        colorShader.render(tmpMatrix, positionVbuffer, positionVbufferCount, GL_TRIANGLES, r, g, b, a);
    }

    private void drawPath() {
        if (path.length < 4) {
            return;
        }
        if (colorShader == null) {
            colorShader = new ColorShader();
        }

        preparePoint(path[0], path[1]);
        float ptx = ftmp1;
        float pty = ftmp2;

        float ptx11 = Float.MIN_VALUE;
        float pty11 = Float.MIN_VALUE;
        float ptx12 = Float.MIN_VALUE;
        float pty12 = Float.MIN_VALUE;
        float ptx21 = Float.MIN_VALUE;
        float pty21 = Float.MIN_VALUE;
        float ptx22 = Float.MIN_VALUE;
        float pty22 = Float.MIN_VALUE;

        for (int i = 2; i < path.length; i += 2) {
            preparePoint(path[i], path[i + 1]);
            float tx = ftmp1;
            float ty = ftmp2;

            float vx = tx - ptx;
            float vy = ty - pty;

            float norm = 1.5f * spSize / (float) Math.sqrt(vx * vx + vy * vy);
            vx *= norm;
            vy *= norm;

            float tx11 = ptx + vy;
            float ty11 = pty - vx;
            float tx12 = ptx - vy;
            float ty12 = pty + vx;
            float tx21 = tx + vy;
            float ty21 = ty - vx;
            float tx22 = tx - vy;
            float ty22 = ty + vx;

            if (ptx11 != Float.MIN_VALUE) {
                fltmp1.add(ptx11, pty11, ptx12, pty12, ptx21, pty21, ptx12, pty12, ptx21, pty21, ptx22, pty22);
                fltmp1.add(ptx21, pty21, tx11, ty11, ptx, pty, ptx22, pty22, tx12, ty12, ptx, pty);
            }

            ptx = tx;
            pty = ty;

            ptx11 = tx11;
            pty11 = ty11;
            ptx12 = tx12;
            pty12 = ty12;
            ptx21 = tx21;
            pty21 = ty21;
            ptx22 = tx22;
            pty22 = ty22;
        }

        fltmp1.add(ptx11, pty11, ptx12, pty12, ptx21, pty21, ptx12, pty12, ptx21, pty21, ptx22, pty22);

        glBindBuffer(GL_ARRAY_BUFFER, pathVbuffer);
        glBufferData(GL_ARRAY_BUFFER, fltmp1.size * 4, FloatBuffer.wrap(fltmp1.data, 0, fltmp1.size), GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        colorShader.render(mapMatrix, pathVbuffer, fltmp1.size / 2, GL_TRIANGLES, 0, 0, 0, 1);

        fltmp1.clear();
    }

    private void preparePoint(double mercatorX, double mercatorY) {
        float x = (float) ((mercatorX - posX) * mercatorPixels);
        float y = (float) ((mercatorY - posY) * mercatorPixels);

        ftmp1 = surfaceCenterX + x * azimuthCos - y * azimuthSin;
        ftmp2 = surfaceCenterY + y * azimuthCos + x * azimuthSin;
    }

    public void moveSingle(float preX, float preY, float postX, float postY) {
        double x = (preX - postX) * mercatorPixelSize;
        double y = (preY - postY) * mercatorPixelSize;
        setPos(posX + x * azimuthCos + y * azimuthSin, posY + y * azimuthCos - x * azimuthSin);

        mapComponent.glView.requestRender();
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

        preX1 -= surfaceCenterX;
        preY1 -= surfaceCenterY;
        preX2 -= surfaceCenterX;
        preY2 -= surfaceCenterY;
        postX1 -= surfaceCenterX;
        postY1 -= surfaceCenterY;
        postX2 -= surfaceCenterX;
        postY2 -= surfaceCenterY;

        double posX1 = posX + (preX1 * azimuthCos + preY1 * azimuthSin) * preMercatorPixelSize;
        double posY1 = posY + (preY1 * azimuthCos - preX1 * azimuthSin) * preMercatorPixelSize;

        double lastAngle = Math.atan2(preX2 - preX1, preY2 - preY1);
        double angle = Math.atan2(postX2 - postX1, postY2 - postY1);
        setAzimuth(azimuth + (float) Math.toDegrees(lastAngle - angle));

        double posX2 = posX1 - (postX1 * azimuthCos + postY1 * azimuthSin) * mercatorPixelSize;
        double posY2 = posY1 - (postY1 * azimuthCos - postX1 * azimuthSin) * mercatorPixelSize;
        setPos(posX2, posY2);

        mapComponent.glView.requestRender();
    }

    public void zoomIn(float x, float y) {
        double preMercatorPixelSize = mercatorPixelSize;

        setZoom(Math.round(zoom + 1));

        x -= surfaceCenterX;
        y -= surfaceCenterY;

        double px = posX + (x * azimuthCos + y * azimuthSin) * (preMercatorPixelSize - mercatorPixelSize);
        double py = posY + (y * azimuthCos - x * azimuthSin) * (preMercatorPixelSize - mercatorPixelSize);
        setPos(px, py);

        mapComponent.glView.requestRender();
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
        } else if (newZoom > 20) {
            newZoom = 20;
        }

        zoom = newZoom;
        refreshZoomValues();
    }

    private void setAzimuth(float newAzimuth) {
        while (newAzimuth < 0) {
            newAzimuth += 360;
        }
        while (newAzimuth > 360) {
            newAzimuth -= 360;
        }

        azimuth = newAzimuth;
        refreshAzimuthValues();
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
        mapComponent.glView.requestRender();
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

    private void refreshAzimuthValues() {
        double rad = Math.toRadians(azimuth);
        azimuthCos = (float) Math.cos(rad);
        azimuthSin = (float) Math.sin(rad);
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
