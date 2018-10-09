package eichlerjiri.mapcomponent;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import eichlerjiri.mapcomponent.shaders.ColorShader;
import eichlerjiri.mapcomponent.shaders.MapShader;
import eichlerjiri.mapcomponent.utils.CachedTile;
import eichlerjiri.mapcomponent.utils.Common;
import eichlerjiri.mapcomponent.utils.FloatArrayList;
import eichlerjiri.mapcomponent.utils.LoadedTile;
import eichlerjiri.mapcomponent.utils.MapTileKeyHashMap;

import static android.opengl.GLES20.*;
import static eichlerjiri.mapcomponent.utils.Common.*;

public class MapComponentRenderer implements GLSurfaceView.Renderer {

    private final MapComponent mapComponent;
    private final float spSize;
    public final float tileSize;

    private final MapTileKeyHashMap<CachedTile> tileCache = new MapTileKeyHashMap<>();

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
    private float zoom = Float.MIN_VALUE;
    private int mapZoom;
    private int tiles;
    private double tilesReversed;
    private double mercatorPixels;
    private float scale;

    // screen-size related values
    private float surfaceCenterX;
    private float surfaceCenterY;
    private int searchDist;
    private final float[] mapMatrix = new float[16];

    // azimuth related values
    private float azimuth = Float.MIN_VALUE;
    private float azimuthSin;
    private float azimuthCos;
    private final float[] rotateMatrix = new float[16];

    // tmps
    private final int[] itmp = new int[1];
    private final FloatArrayList fltmp = new FloatArrayList();
    private FloatBuffer fbtmp;
    private final float[] tmpMatrix = new float[16];
    private final float[] tmpMatrix2 = new float[16];
    private float ftmpX;
    private float ftmpY;

    private MapData d;
    private int tick;

    public MapComponentRenderer(MapComponent mapComponent) {
        this.mapComponent = mapComponent;
        spSize = spSize(mapComponent.getContext());
        tileSize = 256 * spSize;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.8f, 0.97f, 1.0f, 1.0f);

        tileCache.clear();

        mapShader = new MapShader();

        float[] mapBufferData = new float[]{0, 0, 0, 1, 1, 1, 1, 0};
        mapVbuffer = prepareStaticBuffer(mapBufferData, itmp);
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
        d = mapComponent.dCommited;
        tick++;

        if (d.zoom != zoom) {
            refreshZoomValues();
        }
        if (d.azimuth != azimuth) {
            refreshAzimuthValues();
        }

        glClear(GL_COLOR_BUFFER_BIT);

        double centerX = tiles * d.posX;
        double centerY = tiles * d.posY;

        int centerTileX = (int) centerX;
        int centerTileY = (int) centerY;
        if (centerTileY == tiles) {
            centerTileY--;
        }

        mapComponent.tileLoadPool.processLoaded();
        for (int i = centerTileX - searchDist; i <= centerTileX + searchDist; i++) {
            for (int j = centerTileY - searchDist; j <= centerTileY + searchDist; j++) {
                drawTile(i, j, Math.abs(i - centerTileX) + Math.abs(j - centerTileY));
            }
        }
        mapComponent.tileLoadPool.cancelUnused(tick);
        removeUnused();

        if (d.path != null) {
            if (pathVbuffer == 0) {
                glGenBuffers(1, itmp, 0);
                pathVbuffer = itmp[0];
            }
            drawPath(d.path, d.pathOffset, d.pathLength);
        } else if (pathVbuffer != 0) {
            itmp[0] = pathVbuffer;
            glDeleteBuffers(1, itmp, 0);
            pathVbuffer = 0;
        }
        if (d.startPositionX != Double.MIN_VALUE) {
            drawPosition(d.startPositionX, d.startPositionY, 0, 1, 0, 1);
        }
        if (d.endPositionX != Double.MIN_VALUE) {
            drawPosition(d.endPositionX, d.endPositionY, 1, 0, 0, 1);
        }
        if (d.currentPositionX != Double.MIN_VALUE) {
            drawCurrentPosition(d.currentPositionX, d.currentPositionY, d.currentPositionAzimuth);
        }
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

            Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmpX, ftmpY, 0);
            Matrix.scaleM(tmpMatrix, 0, scale, scale, 1);
            Common.multiplyMM(tmpMatrix2, tmpMatrix, rotateMatrix);

            mapShader.render(tmpMatrix2, mapVbuffer, mapVbufferCount, texture, GL_TRIANGLE_FAN, 1, 1, 0, 0);
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
                Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmpX, ftmpY, 0);
                Matrix.scaleM(tmpMatrix, 0, scale, scale, 1);
                Common.multiplyMM(tmpMatrix2, tmpMatrix, rotateMatrix);

                int zoomDiffTiles = 1 << (mapZoom - testZoom);
                float scaleTile = 1.0f / zoomDiffTiles;
                float shiftX = scaleTile * (tileX - testX * zoomDiffTiles);
                float shiftY = scaleTile * (tileY - testY * zoomDiffTiles);

                mapShader.render(tmpMatrix2, mapVbuffer, mapVbufferCount, texture, GL_TRIANGLE_FAN,
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
                    Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmpX, ftmpY, 0);
                    Matrix.scaleM(tmpMatrix, 0, scale * 0.5f, scale * 0.5f, 1);
                    Common.multiplyMM(tmpMatrix2, tmpMatrix, rotateMatrix);

                    mapShader.render(tmpMatrix2, mapVbuffer, mapVbufferCount, texture, GL_TRIANGLE_FAN, 1, 1, 0, 0);
                }
            }
        }
    }

    private int getTexture(int z, int x, int y, int priority) {
        if (z < 0 || z > 19) {
            return 0;
        }

        CachedTile cacheItem = tileCache.get(z, x, y);
        if (cacheItem == null) {
            mapComponent.tileLoadPool.requestTile(z, x, y, tick, priority);
            return 0;
        }

        cacheItem.tick = tick;
        return cacheItem.textureId;
    }

    private void drawCurrentPosition(double x, double y, float positionAzimuth) {
        if (colorShader == null) {
            colorShader = new ColorShader();
        }

        preparePoint(x, y);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmpX, ftmpY, 0);

        if (positionAzimuth != Float.MIN_VALUE) {
            if (currentPositionVbuffer == 0) {
                float[] data = new float[]{-12, 12, 0, -4, 0, -12, 12, 12, 0, -4, 0, -12};
                for (int i = 0; i < data.length; i++) {
                    data[i] *= spSize;
                }
                currentPositionVbuffer = prepareStaticBuffer(data, itmp);
                currentPositionVbufferCount = data.length / 2;
            }

            Matrix.rotateM(tmpMatrix, 0, azimuth + positionAzimuth, 0, 0, 1);

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
                nodirCurrentPositionVbuffer = prepareStaticBuffer(data, itmp);
                nodirCurrentPositionVbufferCount = data.length / 2;
            }

            colorShader.render(tmpMatrix, nodirCurrentPositionVbuffer, nodirCurrentPositionVbufferCount, GL_TRIANGLES,
                    0, 0, 1, 1);
        }
    }

    private void drawPosition(double positionX, double positionY, float r, float g, float b, float a) {
        if (colorShader == null) {
            colorShader = new ColorShader();
        }
        if (positionVbuffer == 0) {
            float[] data = new float[]{0, 0, 10, -20, -10, -20};
            for (int i = 0; i < data.length; i++) {
                data[i] *= spSize;
            }
            positionVbuffer = prepareStaticBuffer(data, itmp);
            positionVbufferCount = data.length / 2;
        }

        preparePoint(positionX, positionY);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmpX, ftmpY, 0);

        colorShader.render(tmpMatrix, positionVbuffer, positionVbufferCount, GL_TRIANGLES, r, g, b, a);
    }

    private void drawPath(double[] path, int offset, int length) {
        if (length < 4) {
            return;
        }
        if (colorShader == null) {
            colorShader = new ColorShader();
        }

        preparePoint(path[offset], path[offset + 1]);
        float ptx = ftmpX;
        float pty = ftmpY;

        float ptx11 = Float.MIN_VALUE;
        float pty11 = Float.MIN_VALUE;
        float ptx12 = Float.MIN_VALUE;
        float pty12 = Float.MIN_VALUE;
        float ptx21 = Float.MIN_VALUE;
        float pty21 = Float.MIN_VALUE;
        float ptx22 = Float.MIN_VALUE;
        float pty22 = Float.MIN_VALUE;

        for (int i = 2; i < length; i += 2) {
            preparePoint(path[offset + i], path[offset + i + 1]);
            float tx = ftmpX;
            float ty = ftmpY;

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
                fltmp.add(ptx11, pty11, ptx12, pty12, ptx21, pty21, ptx12, pty12, ptx21, pty21, ptx22, pty22);
                fltmp.add(ptx21, pty21, tx11, ty11, ptx, pty, ptx22, pty22, tx12, ty12, ptx, pty);
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

        fltmp.add(ptx11, pty11, ptx12, pty12, ptx21, pty21, ptx12, pty12, ptx21, pty21, ptx22, pty22);

        if (fbtmp == null || fbtmp.array() != fltmp.data) {
            fbtmp = FloatBuffer.wrap(fltmp.data);
        }

        glBindBuffer(GL_ARRAY_BUFFER, pathVbuffer);
        glBufferData(GL_ARRAY_BUFFER, fltmp.size * 4, fbtmp, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        colorShader.render(mapMatrix, pathVbuffer, fltmp.size / 2, GL_TRIANGLES, 0, 0, 0, 1);

        fltmp.clear();
    }

    private void preparePoint(double mercatorX, double mercatorY) {
        float x = (float) ((mercatorX - d.posX) * mercatorPixels);
        float y = (float) ((mercatorY - d.posY) * mercatorPixels);

        ftmpX = surfaceCenterX + x * azimuthCos - y * azimuthSin;
        ftmpY = surfaceCenterY + y * azimuthCos + x * azimuthSin;
    }

    public void tileLoaded(LoadedTile loadedTile) {
        if (loadedTile.data == null) {
            tileCache.put(loadedTile.tileKey, new CachedTile(0, tick));
            return;
        }

        glGenTextures(1, itmp, 0);
        int textureId = itmp[0];

        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, loadedTile.width, loadedTile.height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                loadedTile.data);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);

        tileCache.put(loadedTile.tileKey, new CachedTile(textureId, tick));
    }

    private void removeUnused() {
        for (int i = 0; i < tileCache.values.length; i++) {
            CachedTile item = (CachedTile) tileCache.values[i];
            if (item != null) {
                if (item.tick != tick) {
                    if (item.textureId != 0) {
                        itmp[0] = item.textureId;
                        glDeleteTextures(1, itmp, 0);
                    }
                    tileCache.remove(i--);
                }
            }
        }
    }

    private void refreshScreenSizeValues() {
        surfaceCenterX = w * 0.5f;
        surfaceCenterY = h * 0.5f;

        int tilesNecessaryBase = (int) (Math.sqrt(w * w + h * h) / tileSize);
        searchDist = 1 + tilesNecessaryBase / 2;

        if (w != 0 && h != 0) {
            Matrix.orthoM(mapMatrix, 0, 0, w, h, 0, -10, 10);
        } else {
            Matrix.setIdentityM(mapMatrix, 0);
        }
    }

    private void refreshZoomValues() {
        zoom = d.zoom;
        mapZoom = (int) zoom;
        tiles = 1 << mapZoom;
        tilesReversed = 1 / (double) tiles;
        mercatorPixels = tileSize * Math.pow(2, zoom);
        scale = (float) (mercatorPixels * tilesReversed);
    }

    private void refreshAzimuthValues() {
        azimuth = d.azimuth;

        double rad = Math.toRadians(azimuth);
        azimuthCos = (float) Math.cos(rad);
        azimuthSin = (float) Math.sin(rad);

        Matrix.setRotateM(rotateMatrix, 0, azimuth, 0, 0, 1);
    }
}
