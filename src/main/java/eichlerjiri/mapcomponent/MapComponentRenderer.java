package eichlerjiri.mapcomponent;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.io.Closeable;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import eichlerjiri.mapcomponent.utils.BitmapRGB;
import eichlerjiri.mapcomponent.utils.MapTileKey;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;

public class MapComponentRenderer implements GLSurfaceView.Renderer, Closeable {

    private final MapComponent mapComponent;

    private final TileLoader tileLoader = new TileLoader(this);
    private final HashMap<MapTileKey, Integer> tileCache = new HashMap<>();

    private double posX = 0.5;
    private double posY = 0.5;
    private float zoom = 3;

    private int w;
    private int h;
    private float surfaceCenterX;
    private float surfaceCenterY;
    private float[] terrainMatrix = new float[16];


    public MapComponentRenderer(MapComponent mapComponent) {
        this.mapComponent = mapComponent;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.8f, 0.97f, 1.0f, 1.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        w = width;
        h = height;
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

    public void tileLoaded(MapTileKey key, BitmapRGB bitmap) {
        // nasypat do GL

        mapComponent.requestRender();
    }

    public MapComponent getMapComponent() {
        return mapComponent;
    }

    @Override
    public void close() {
        tileLoader.shutdownNow();
    }
}
