package eichlerjiri.mapcomponent;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class MapComponent extends GLSurfaceView {

    public MapComponent(Context context) {
        super(context);

        setEGLContextClientVersion(2);
        setRenderer(new MapComponentRenderer(this));
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void onDestroy() {

    }
}
