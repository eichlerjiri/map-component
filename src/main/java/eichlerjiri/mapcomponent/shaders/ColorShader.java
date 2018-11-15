package eichlerjiri.mapcomponent.shaders;

import static android.opengl.GLES20.*;
import static eichlerjiri.mapcomponent.utils.Common.*;

public class ColorShader {

    public final int program;

    public final int vertexLoc;
    public final int pvmLoc;
    public final int colorLoc;

    public ColorShader() {
        String vertexShaderSource = "" +
                "attribute vec2 vertex;\n" +
                "uniform mat4 pvm;\n" +
                "\n" +
                "void main() {\n" +
                "    gl_Position = pvm * vec4(vertex, 0, 1);\n" +
                "}\n";

        String fragmentShaderSource = "precision mediump float;\n" +
                "uniform vec4 color;\n" +
                "\n" +
                "void main() {\n" +
                "    gl_FragColor = color;\n" +
                "}\n";

        program = createProgram(vertexShaderSource, fragmentShaderSource);

        vertexLoc = glGetAttribLocation(program, "vertex");
        pvmLoc = glGetUniformLocation(program, "pvm");
        colorLoc = glGetUniformLocation(program, "color");
    }
}
