package eichlerjiri.map;

import static android.opengl.GLES20.*;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLMapRenderer implements GLSurfaceView.Renderer {

    private double posX = 0.5;
    private double posY = 0.5;
    private float zoom = 3;

    private int width;
    private int height;
    private float surfaceCenterX;
    private float surfaceCenterY;
    private float[] terrainMatrix = new float[16];

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.8f, 0.97f, 1.0f, 1.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        surfaceCenterX = width * 0.5f;
        surfaceCenterY = height * 0.5f;
        Matrix.orthoM(terrainMatrix, 0, -surfaceCenterX, surfaceCenterX, -surfaceCenterY, surfaceCenterY, -10, 10);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT);

        int mapZoom = (int) zoom;

        int tiles = 1 << mapZoom;
        double centerX = tiles * posX;
        double centerY = tiles * posY;

        int tileX = (int) centerX;
        int tileY = (int) centerY;

        float centerShiftX = (float) (centerX - tileX);
        float centerShiftY = (float) (centerY - tileY);

        float centerTileX = surfaceCenterX - centerShiftX;
        float centerTileY = surfaceCenterY - centerShiftY;

    }
}
