package eichlerjiri.mapcomponent;

import static android.opengl.GLES20.*;
import android.opengl.Matrix;
import eichlerjiri.mapcomponent.shaders.ColorShader;
import eichlerjiri.mapcomponent.shaders.MapShader;
import static eichlerjiri.mapcomponent.utils.Common.*;
import eichlerjiri.mapcomponent.utils.FloatList;
import eichlerjiri.mapcomponent.utils.ObjectMap;
import eichlerjiri.mapcomponent.utils.TileKey;
import eichlerjiri.mapcomponent.utils.TileKey.CachedTile;
import eichlerjiri.mapcomponent.utils.TileKey.LoadedTile;
import static java.lang.Math.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicReference;

public class MapComponentRenderer {

    public final MapComponent mapComponent;
    public AtomicReference<MapDataRenderer> dr = new AtomicReference<>(new MapDataRenderer(0, 0, 0, 0, 0, 0));

    public MapData d;
    public int tick;

    // zoom-related values
    public float zoom = Float.NEGATIVE_INFINITY;
    public int mapZoom;
    public int tiles;
    public double tilesReversed;
    public double mercatorPixels;
    public float scale;

    // screen-size related values
    public float surfaceCenterX;
    public float surfaceCenterY;
    public int searchDist;
    public final float[] mapMatrix = new float[16];

    // azimuth related values
    public float azimuth = Float.NEGATIVE_INFINITY;
    public float azimuthSin;
    public float azimuthCos;
    public final float[] rotateMatrix = new float[16];

    // OpenGL objects
    public final ObjectMap<CachedTile> tileCache = new ObjectMap<>(CachedTile.class);
    public int emptyTileTexture;

    public MapShader mapShader;
    public ColorShader colorShader;
    public int squareVbuffer;
    public int squareVbufferCount;

    public int currentPositionVbuffer;
    public int currentPositionVbufferCount;
    public int nodirCurrentPositionVbuffer;
    public int nodirCurrentPositionVbufferCount;
    public int positionVbuffer;
    public int positionVbufferCount;
    public int pathVbuffer;
    public FloatBuffer pathVtmp;

    // tmps
    public final FloatList fltmp = new FloatList();
    public final float[] tmpMatrix = new float[16];
    public final float[] tmpMatrix2 = new float[16];
    public float ftmpX;
    public float ftmpY;
    public final int[] itmp = new int[1];
    public final TileKey<?> tkey = new TileKey<>(0, 0, 0);

    public MapComponentRenderer(MapComponent mapComponent) {
        this.mapComponent = mapComponent;

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public void setDimensions(int width, int height) {
        glViewport(0, 0, width, height);

        int pad = round(10 * mapComponent.spSize);
        int dim = round(40 * mapComponent.spSize);
        dr.set(new MapDataRenderer(width, height, width - pad - dim, pad, dim, dim));

        refreshScreenSizeValues();
    }

    public void drawFrame() {
        d = mapComponent.dCommited.get();
        tick++;

        if (d.zoom != zoom) {
            refreshZoomValues();
        }
        if (d.azimuth != azimuth) {
            refreshAzimuthValues();
        }

        double centerX = tiles * d.posX;
        double centerY = tiles * d.posY;

        int centerTileX = (int) centerX;
        int centerTileY = (int) centerY;
        if (centerTileY == tiles) {
            centerTileY--;
        }

        mapComponent.tileLoadPool.processLoaded();

        if (mapShader == null) {
            mapShader = new MapShader();
            colorShader = new ColorShader();

            float[] mapBufferData = new float[]{0, 0, 0, 1, 1, 1, 1, 0};
            squareVbuffer = prepareStaticBuffer(mapBufferData);
            squareVbufferCount = mapBufferData.length / 2;
        }

        glUseProgram(mapShader.program);
        glEnableVertexAttribArray(mapShader.vertexLoc);
        glBindBuffer(GL_ARRAY_BUFFER, squareVbuffer);
        glVertexAttribPointer(mapShader.vertexLoc, 2, GL_FLOAT, false, 0, 0);

        for (int i = centerTileX - searchDist; i <= centerTileX + searchDist; i++) {
            for (int j = centerTileY - searchDist; j <= centerTileY + searchDist; j++) {
                drawTile(i, j, abs(i - centerTileX) + abs(j - centerTileY));
            }
        }

        mapComponent.tileLoadPool.cancelUnused(tick);

        removeUnused();

        glUseProgram(colorShader.program);
        glEnableVertexAttribArray(colorShader.vertexLoc);

        if (d.path != null && d.pathLength >= 4) {
            drawPath(d.path, d.pathOffset, d.pathLength);
        }
        if (d.startPositionX != Double.NEGATIVE_INFINITY) {
            drawPosition(d.startPositionX, d.startPositionY, 0, 1, 0, 1);
        }
        if (d.endPositionX != Double.NEGATIVE_INFINITY) {
            drawPosition(d.endPositionX, d.endPositionY, 1, 0, 0, 1);
        }
        if (d.currentPositionX != Double.NEGATIVE_INFINITY) {
            drawCurrentPosition(d.currentPositionX, d.currentPositionY, d.currentPositionAzimuth);
        }

        if (!d.centered) {
            drawCenterButton();
        }
    }

    public void drawTile(int tileXview, int tileY, int toCenter) {
        int tileX = tileXview;
        while (tileX < 0) {
            tileX += tiles;
        }
        while (tileX >= tiles) {
            tileX -= tiles;
        }

        // no tile - pole areas. So we don't need to call glClear
        if (tileY < 0 || tileY >= tiles) {
            drawTileEmpty(tileXview, tileY);
            return;
        }

        // regular tile, matching zoom
        int texture = getTexture(mapZoom, tileX, tileY, toCenter);
        if (texture != 0) {
            preparePoint(tileXview * tilesReversed, tileY * tilesReversed);
            Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmpX, ftmpY, 0);
            Matrix.scaleM(tmpMatrix, 0, scale, scale, 1);
            multiplyMM(tmpMatrix2, tmpMatrix, rotateMatrix);

            renderTile(tmpMatrix2, texture, 1, 1, 0, 0);
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
                multiplyMM(tmpMatrix2, tmpMatrix, rotateMatrix);

                int zoomDiffTiles = 1 << (mapZoom - testZoom);
                float scaleTile = 1.0f / zoomDiffTiles;
                float shiftX = scaleTile * (tileX - testX * zoomDiffTiles);
                float shiftY = scaleTile * (tileY - testY * zoomDiffTiles);

                renderTile(tmpMatrix2, texture, scaleTile, scaleTile, shiftX, shiftY);
                return;
            }

            testZoom--;
            testX /= 2;
            testY /= 2;
        }

        drawTileEmpty(tileXview, tileY);

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
                    multiplyMM(tmpMatrix2, tmpMatrix, rotateMatrix);

                    renderTile(tmpMatrix2, texture, 1, 1, 0, 0);
                }
            }
        }
    }

    public void drawTileEmpty(int tileXview, int tileY) {
        preparePoint(tileXview * tilesReversed, tileY * tilesReversed);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmpX, ftmpY, 0);
        Matrix.scaleM(tmpMatrix, 0, scale, scale, 1);
        multiplyMM(tmpMatrix2, tmpMatrix, rotateMatrix);

        if (emptyTileTexture == 0) {
            emptyTileTexture = prepareTexture(1, 1, ByteBuffer.wrap(new byte[]{(byte) 204, (byte) 247, (byte) 255, (byte) 255}));
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        }

        renderTile(tmpMatrix2, emptyTileTexture, 1, 1, 0, 0);
    }

    public int getTexture(int z, int x, int y, int priority) {
        if (z < 0 || z > 19) {
            return 0;
        }

        CachedTile cacheItem = tileCache.get(tkey.value(z, x, y));
        if (cacheItem == null) {
            mapComponent.tileLoadPool.requestTile(z, x, y, tick, priority);
            return 0;
        }

        cacheItem.tick = tick;
        return cacheItem.texture;
    }

    public void renderTile(float[] pvm, int texture, float scaleX, float scaleY, float shiftX, float shiftY) {
        glBindTexture(GL_TEXTURE_2D, texture);

        glUniform4f(mapShader.scaleShiftLoc, scaleX, scaleY, shiftX, shiftY);
        glUniformMatrix4fv(mapShader.pvmLoc, 1, false, pvm, 0);
        glDrawArrays(GL_TRIANGLE_FAN, 0, squareVbufferCount);
    }

    public void drawCurrentPosition(double x, double y, float positionAzimuth) {
        preparePoint(x, y);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmpX, ftmpY, 0);

        if (positionAzimuth != Float.NEGATIVE_INFINITY) {
            Matrix.rotateM(tmpMatrix, 0, azimuth + positionAzimuth, 0, 0, 1);

            if (currentPositionVbuffer == 0) {
                float[] data = prepareSpData(new float[]{-12, 12, 0, -4, 0, -12, 12, 12, 0, -4, 0, -12});
                currentPositionVbuffer = prepareStaticBuffer(data);
                currentPositionVbufferCount = data.length / 2;
            }

            renderColor(tmpMatrix, currentPositionVbuffer, currentPositionVbufferCount, GL_TRIANGLES, 0, 0, 1, 1);
        } else {
            prepareNodirCurrentPositionVbuffer();
            renderColor(tmpMatrix, nodirCurrentPositionVbuffer, nodirCurrentPositionVbufferCount, GL_TRIANGLES, 0, 0, 1, 1);
        }
    }

    public void drawPosition(double positionX, double positionY, float r, float g, float b, float a) {
        preparePoint(positionX, positionY);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmpX, ftmpY, 0);

        if (positionVbuffer == 0) {
            float[] data = prepareSpData(new float[]{0, 0, 10, -20, -10, -20});
            positionVbuffer = prepareStaticBuffer(data);
            positionVbufferCount = data.length / 2;
        }

        renderColor(tmpMatrix, positionVbuffer, positionVbufferCount, GL_TRIANGLES, r, g, b, a);
    }

    public void drawPath(double[] path, int offset, int length) {
        preparePoint(path[offset], path[offset + 1]);
        float ptx = ftmpX;
        float pty = ftmpY;

        float ptx11 = Float.NEGATIVE_INFINITY;
        float pty11 = Float.NEGATIVE_INFINITY;
        float ptx12 = Float.NEGATIVE_INFINITY;
        float pty12 = Float.NEGATIVE_INFINITY;
        float ptx21 = Float.NEGATIVE_INFINITY;
        float pty21 = Float.NEGATIVE_INFINITY;
        float ptx22 = Float.NEGATIVE_INFINITY;
        float pty22 = Float.NEGATIVE_INFINITY;

        for (int i = 2; i < length; i += 2) {
            preparePoint(path[offset + i], path[offset + i + 1]);
            float tx = ftmpX;
            float ty = ftmpY;

            float vx = tx - ptx;
            float vy = ty - pty;

            float norm = 1.5f * mapComponent.spSize / (float) sqrt(vx * vx + vy * vy);
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

            if (ptx11 != Float.NEGATIVE_INFINITY) {
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

        if (pathVbuffer == 0) {
            glGenBuffers(1, itmp, 0);
            pathVbuffer = itmp[0];
        }

        if (pathVtmp == null || pathVtmp.array() != fltmp.data) {
            pathVtmp = FloatBuffer.wrap(fltmp.data);
        }

        glBindBuffer(GL_ARRAY_BUFFER, pathVbuffer);
        glBufferData(GL_ARRAY_BUFFER, fltmp.size * 4, pathVtmp, GL_STREAM_DRAW);
        renderColor(mapMatrix, pathVbuffer, fltmp.size / 2, GL_TRIANGLES, 0, 0, 0, 1);

        fltmp.size = 0;
    }

    public void drawCenterButton() {
        MapDataRenderer dr = this.dr.get();

        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, dr.centerButtonX, dr.centerButtonY, 0);
        Matrix.scaleM(tmpMatrix, 0, dr.centerButtonWidth, dr.centerButtonHeight, 1);

        renderColor(tmpMatrix, squareVbuffer, squareVbufferCount, GL_TRIANGLE_FAN, 1, 1, 1, 0.6f);

        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, dr.centerButtonX + dr.centerButtonWidth / 2, dr.centerButtonY + dr.centerButtonHeight / 2, 0);
        Matrix.scaleM(tmpMatrix, 0, 1.3f, 1.3f, 1);

        prepareNodirCurrentPositionVbuffer();
        renderColor(tmpMatrix, nodirCurrentPositionVbuffer, nodirCurrentPositionVbufferCount, GL_TRIANGLES, 0, 0, 1, 1);
    }

    public void renderColor(float[] pvm, int buffer, int bufferCount, int type, float r, float g, float b, float a) {
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glVertexAttribPointer(colorShader.vertexLoc, 2, GL_FLOAT, false, 0, 0);

        glUniform4f(colorShader.colorLoc, r, g, b, a);
        glUniformMatrix4fv(colorShader.pvmLoc, 1, false, pvm, 0);
        glDrawArrays(type, 0, bufferCount);
    }

    public void preparePoint(double mercatorX, double mercatorY) {
        float x = (float) ((mercatorX - d.posX) * mercatorPixels);
        float y = (float) ((mercatorY - d.posY) * mercatorPixels);

        ftmpX = surfaceCenterX + x * azimuthCos - y * azimuthSin;
        ftmpY = surfaceCenterY + y * azimuthCos + x * azimuthSin;
    }

    public void prepareNodirCurrentPositionVbuffer() {
        if (nodirCurrentPositionVbuffer == 0) {
            float[] data = prepareSpData(new float[]{
                    -12, 0, -8, 0, 0, 12, 0, 12, 0, 8, -8, 0,
                    12, 0, 8, 0, 0, -12, 0, -12, 0, -8, 8, 0,
                    -12, 0, -8, 0, 0, -12, 0, -12, 0, -8, -8, 0,
                    12, 0, 8, 0, 0, 12, 0, 12, 0, 8, 8, 0
            });
            nodirCurrentPositionVbuffer = prepareStaticBuffer(data);
            nodirCurrentPositionVbufferCount = data.length / 2;
        }
    }

    public void tileLoaded(LoadedTile loadedTile) {
        if (loadedTile.data == null) {
            tileCache.put(new CachedTile(loadedTile.zoom, loadedTile.x, loadedTile.y, 0, tick));
            return;
        }

        int texture = prepareTexture(loadedTile.width, loadedTile.height, loadedTile.data);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        tileCache.put(new CachedTile(loadedTile.zoom, loadedTile.x, loadedTile.y, texture, tick));
    }

    public float[] prepareSpData(float[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] *= mapComponent.spSize;
        }
        return data;
    }

    public int prepareStaticBuffer(float[] data) {
        glGenBuffers(1, itmp, 0);
        int buffer = itmp[0];

        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glBufferData(GL_ARRAY_BUFFER, data.length * 4, FloatBuffer.wrap(data), GL_STATIC_DRAW);

        return buffer;
    }

    public int prepareTexture(int width, int height, ByteBuffer data) {
        glGenTextures(1, itmp, 0);
        int texture = itmp[0];

        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);

        return texture;
    }

    public void removeUnused() {
        for (int i = 0; i < tileCache.data.length; i++) {
            CachedTile entry = tileCache.data[i];
            while (entry != null) {
                if (entry.tick != tick) {
                    if (entry.texture != 0) {
                        itmp[0] = entry.texture;
                        glDeleteTextures(1, itmp, 0);
                    }
                    tileCache.remove(entry);
                }
                entry = entry.next;
            }
        }
    }

    public void refreshScreenSizeValues() {
        MapDataRenderer dr = this.dr.get();

        surfaceCenterX = dr.width * 0.5f;
        surfaceCenterY = dr.height * 0.5f;

        int tilesNecessaryBase = (int) (sqrt(dr.width * dr.width + dr.height * dr.height) / mapComponent.tileSize);
        searchDist = 1 + tilesNecessaryBase / 2;

        if (dr.width != 0 && dr.height != 0) {
            Matrix.orthoM(mapMatrix, 0, 0, dr.width, dr.height, 0, -10, 10);
        } else {
            Matrix.setIdentityM(mapMatrix, 0);
        }
    }

    public void refreshZoomValues() {
        zoom = d.zoom;
        mapZoom = (int) zoom;
        tiles = 1 << mapZoom;
        tilesReversed = 1 / (double) tiles;
        mercatorPixels = mapComponent.tileSize * pow(2, zoom);
        scale = (float) (mercatorPixels * tilesReversed);
    }

    public void refreshAzimuthValues() {
        azimuth = d.azimuth;

        double rad = toRadians(azimuth);
        azimuthCos = (float) cos(rad);
        azimuthSin = (float) sin(rad);

        Matrix.setRotateM(rotateMatrix, 0, azimuth, 0, 0, 1);
    }
}
