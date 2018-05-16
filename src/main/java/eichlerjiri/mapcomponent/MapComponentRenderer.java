package eichlerjiri.mapcomponent;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import eichlerjiri.mapcomponent.shaders.MapShader;
import eichlerjiri.mapcomponent.utils.MapTileKey;

import static android.opengl.GLES20.*;

public class MapComponentRenderer implements GLSurfaceView.Renderer {

    private static final float tileSize = 256.0f;

    private final MapComponent mapComponent;

    public double posX = 0.5;
    public double posY = 0.5;
    public float zoom = 4.0f;

    private final HashMap<MapTileKey, Integer> tileCache = new HashMap<>();
    private final MapTileKey testKey = new MapTileKey();

    private MapShader mapShader;
    private int mapVbuffer;
    private int mapVbufferCount;

    private int w;
    private int h;
    private float surfaceCenterX;
    private float surfaceCenterY;
    private int searchDist;
    private final float[] mapMatrix = new float[16];

    private final int[] itmp1 = new int[1];
    private final float[] tmpMatrix = new float[16];

    public MapComponentRenderer(MapComponent mapComponent) {
        this.mapComponent = mapComponent;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.8f, 0.97f, 1.0f, 1.0f);

        tileCache.clear();

        mapShader = new MapShader();

        glGenBuffers(1, itmp1, 0);
        mapVbuffer = itmp1[0];

        glBindBuffer(GL_ARRAY_BUFFER, mapVbuffer);

        float[] data = new float[]{0, 0, 0, 1, 1, 1, 1, 0};
        glBufferData(GL_ARRAY_BUFFER, data.length * 4, FloatBuffer.wrap(data), GL_STATIC_DRAW);
        mapVbufferCount = data.length / 2;

        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);

        w = width;
        h = height;
        surfaceCenterX = width * 0.5f;
        surfaceCenterY = height * 0.5f;

        int tilesNecessaryBase = (int) (Math.sqrt(width * width + height * height) / tileSize);
        searchDist = (tilesNecessaryBase + 1) / 2;

        Matrix.orthoM(mapMatrix, 0, 0, width, height, 0, -10, 10);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT);

        int mapZoom = (int) zoom;
        int tiles = 1 << mapZoom;

        float scale = (float) (mercatorPixels() / tiles);

        double centerX = tiles * posX;
        double centerY = tiles * posY;

        int centerTileX = (int) centerX;
        int centerTileY = (int) centerY;
        if (centerTileY == tiles) {
            centerTileY--;
        }

        for (int i = centerTileX - searchDist; i <= centerTileX + searchDist; i++) {
            for (int j = centerTileY - searchDist; j <= centerTileY + searchDist; j++) {
                drawTile(mapZoom, i, j, tiles, centerX, centerY, scale);
            }
        }
    }

    private void drawTile(int mapZoom, int tileX, int tileY, int tiles, double centerX, double centerY, float scale) {
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

        testKey.changeTo(mapZoom, tileXreal, tileY);
        Integer textureId = tileCache.get(testKey);

        if (textureId == null) {
            mapComponent.tileLoader.requestTile(new MapTileKey(mapZoom, tileXreal, tileY));
            return;
        }
        if (textureId == 0) {
            return;
        }

        float centerShiftX = (float) ((centerX - tileX) * tileSize);
        float centerShiftY = (float) ((centerY - tileY) * tileSize);

        float translateX = surfaceCenterX - centerShiftX;
        float translateY = surfaceCenterY - centerShiftY;

        glUseProgram(mapShader.programId);
        glEnableVertexAttribArray(mapShader.vertexLoc);

        glBindBuffer(GL_ARRAY_BUFFER, mapVbuffer);
        glVertexAttribPointer(mapShader.vertexLoc, 2, GL_FLOAT, false, 0, 0);

        Matrix.translateM(tmpMatrix, 0, mapMatrix, 0, translateX, translateY, 0);
        Matrix.scaleM(tmpMatrix, 0, scale, scale, 1);
        glUniformMatrix4fv(mapShader.pvmLoc, 1, false, tmpMatrix, 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);

        glDrawArrays(GL_TRIANGLE_FAN, 0, mapVbufferCount);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDisableVertexAttribArray(mapShader.vertexLoc);
        glUseProgram(0);
    }

    public void moveByPixels(float diffX, float diffY) {
        double mercatorPixelSize = 1 / mercatorPixels();
        posX += diffX * mercatorPixelSize;
        posY += diffY * mercatorPixelSize;

        while (posX < 0) {
            posX++;
        }
        while (posX > 1) {
            posX--;
        }

        if (posY < 0) {
            posY = 0;
        } else if (posY > 1) {
            posY = 1;
        }

        mapComponent.requestRender();
    }

    public void tileLoaded(MapTileKey key, int width, int height, ByteBuffer data) {
        if (data == null) {
            tileCache.put(key, 0);
            return;
        }

        glGenTextures(1, itmp1, 0);
        int textureId = itmp1[0];

        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glBindTexture(GL_TEXTURE_2D, 0);

        tileCache.put(key, textureId);
        mapComponent.requestRender();
    }

    private double mercatorPixels() {
        return tileSize * Math.pow(2, zoom);
    }
}
