package eichlerjiri.map;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class GLMap extends GLSurfaceView {

    public GLMap(Context context) {
        super(context);

        setEGLContextClientVersion(2);
        setRenderer(new GLMapRenderer());
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
