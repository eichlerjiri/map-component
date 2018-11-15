package eichlerjiri.mapcomponent;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import eichlerjiri.mapcomponent.utils.CachedTile;
import eichlerjiri.mapcomponent.utils.Common;
import eichlerjiri.mapcomponent.utils.FloatArrayList;
import eichlerjiri.mapcomponent.utils.LoadedTile;
import eichlerjiri.mapcomponent.utils.TileKeyHashMap;

import static eichlerjiri.mapcomponent.utils.Common.*;

public class MapComponentRenderer implements GLSurfaceView.Renderer {

    private final MapComponent mapComponent;
    private final float spSize;
    public final float tileSize;

    private final TileKeyHashMap<CachedTile> tileCache = new TileKeyHashMap<>();
    private Drawing drawing;
    private MapData d;
    private int tick;

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
    private final FloatArrayList fltmp = new FloatArrayList();
    private final float[] tmpMatrix = new float[16];
    private final float[] tmpMatrix2 = new float[16];
    private float ftmpX;
    private float ftmpY;

    public MapComponentRenderer(MapComponent mapComponent) {
        this.mapComponent = mapComponent;
        spSize = spSize(mapComponent.getContext());
        tileSize = 256 * spSize;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig config) {
        tileCache.clear();
        drawing = new Drawing(spSize);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        drawing.surfaceChanged(width, height);

        w = width;
        h = height;

        refreshScreenSizeValues();
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        d = mapComponent.dCommited;
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
        for (int i = centerTileX - searchDist; i <= centerTileX + searchDist; i++) {
            for (int j = centerTileY - searchDist; j <= centerTileY + searchDist; j++) {
                drawTile(i, j, Math.abs(i - centerTileX) + Math.abs(j - centerTileY));
            }
        }
        mapComponent.tileLoadPool.cancelUnused(tick);
        removeUnused();

        if (d.path != null && d.pathLength >= 4) {
            drawPath(d.path, d.pathOffset, d.pathLength);
        } else {
            drawing.noRenderPath();
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
            Common.multiplyMM(tmpMatrix2, tmpMatrix, rotateMatrix);

            drawing.renderTile(tmpMatrix2, texture, 1, 1, 0, 0);
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

                drawing.renderTile(tmpMatrix2, texture, scaleTile, scaleTile, shiftX, shiftY);
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
                    Common.multiplyMM(tmpMatrix2, tmpMatrix, rotateMatrix);

                    drawing.renderTile(tmpMatrix2, texture, 1, 1, 0, 0);
                }
            }
        }
    }

    private void drawTileEmpty(int tileXview, int tileY) {
        preparePoint(tileXview * tilesReversed, tileY * tilesReversed);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmpX, ftmpY, 0);
        Matrix.scaleM(tmpMatrix, 0, scale, scale, 1);
        Common.multiplyMM(tmpMatrix2, tmpMatrix, rotateMatrix);

        drawing.renderEmptyTile(tmpMatrix2);
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
        preparePoint(x, y);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmpX, ftmpY, 0);

        if (positionAzimuth != Float.MIN_VALUE) {
            Matrix.rotateM(tmpMatrix, 0, azimuth + positionAzimuth, 0, 0, 1);

            drawing.renderCurrentPosition(tmpMatrix);
        } else {
            drawing.renderCurrentPositionNoDir(tmpMatrix);
        }
    }

    private void drawPosition(double positionX, double positionY, float r, float g, float b, float a) {
        preparePoint(positionX, positionY);
        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, ftmpX, ftmpY, 0);

        drawing.renderPosition(tmpMatrix, r, g, b, a);
    }

    private void drawPath(double[] path, int offset, int length) {
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

        drawing.renderPath(mapMatrix, fltmp);

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
            tileCache.put(loadedTile, new CachedTile(0, tick));
            return;
        }

        int textureId = drawing.prepareMapTexture(loadedTile.width, loadedTile.height, loadedTile.data);
        tileCache.put(loadedTile, new CachedTile(textureId, tick));
    }

    private void removeUnused() {
        for (TileKeyHashMap.Entry<CachedTile> entry : tileCache.entries) {
            while (entry != null) {
                CachedTile item = entry.value;
                if (item.tick != tick) {
                    if (item.textureId != 0) {
                        drawing.removeMapTexture(item.textureId);
                    }
                    tileCache.remove(entry);
                }
                entry = entry.next;
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
