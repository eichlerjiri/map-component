package eichlerjiri.mapcomponent.shaders;

import eichlerjiri.mapcomponent.utils.GLUtils;

import static android.opengl.GLES20.*;

public class ColorShader {

    private static final String vertexShaderSource = "" +
            "attribute vec2 vertex;\n" +
            "uniform mat4 pvm;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = pvm * vec4(vertex,0,1);\n" +
            "}\n";

    private static final String fragmentShaderSource = "" +
            "uniform vec4 color;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = color;\n" +
            "}\n";

    public final int programId;

    public final int vertexLoc;
    public final int pvmLoc;
    public final int colorLoc;

    public ColorShader() {
        programId = GLUtils.createProgram(vertexShaderSource, fragmentShaderSource);

        vertexLoc = glGetAttribLocation(programId, "vertex");
        pvmLoc = glGetUniformLocation(programId, "pvm");
        colorLoc = glGetUniformLocation(programId, "color");
    }
}
