package eichlerjiri.mapcomponent;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import eichlerjiri.mapcomponent.shaders.MapShader;
import eichlerjiri.mapcomponent.utils.MapTileKey;

import static android.opengl.GLES20.*;

public class MapComponentRenderer implements GLSurfaceView.Renderer {

    private final MapComponent mapComponent;

    private final HashMap<MapTileKey, Integer> tileCache = new HashMap<>();
    private final MapTileKey testKey = new MapTileKey();

    private double posX = 0.5;
    private double posY = 0.5;
    private float zoom = 1f;

    private MapShader mapShader;
    private int mapVbuffer;
    private int mapVbufferCount;

    private int w;
    private int h;
    private float surfaceCenterX;
    private float surfaceCenterY;
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
        Matrix.orthoM(mapMatrix, 0, 0, width, height, 0, -10, 10);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT);

        int mapZoom = (int) zoom;
        int tiles = 1 << mapZoom;

        double equatorPixels = 256.0 * Math.pow(2, zoom);
        float scale = (float) (equatorPixels / tiles);

        double centerX = tiles * posX;
        double centerY = tiles * posY;

        int tileX = (int) centerX;
        int tileY = (int) centerY;

        float centerShiftX = (float) ((centerX - tileX) * equatorPixels);
        float centerShiftY = (float) ((centerY - tileY) * equatorPixels);

        float translateX = surfaceCenterX - centerShiftX;
        float translateY = surfaceCenterY - centerShiftY;

        testKey.changeTo(mapZoom, tileX, tileY);
        Integer textureId = tileCache.get(testKey);

        if (textureId == null) {
            mapComponent.tileLoader.requestTile(new MapTileKey(mapZoom, tileX, tileY));
        } else {
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
}
