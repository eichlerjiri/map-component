package eichlerjiri.mapcomponent.shaders;

import eichlerjiri.mapcomponent.utils.GLUtils;

import static android.opengl.GLES20.*;

public class MapShader {

    private static final String vertexShaderSource = "" +
            "attribute vec2 vertex;\n" +
            "uniform mat4 pvm;\n" +
            "varying vec2 texCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    texCoord = vertex;\n" +
            "    gl_Position = pvm * vec4(vertex,0,1);\n" +
            "}\n";

    private static final String fragmentShaderSource = "" +
            "varying vec2 texCoord;\n" +
            "uniform sampler2D texture;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(texture, texCoord);\n" +
            "}\n";

    public final int programId;

    public final int vertexLoc;
    public final int pvmLoc;

    public MapShader() {
        programId = GLUtils.createProgram(vertexShaderSource, fragmentShaderSource);

        vertexLoc = glGetAttribLocation(programId, "vertex");
        pvmLoc = glGetUniformLocation(programId, "pvm");
    }
}
