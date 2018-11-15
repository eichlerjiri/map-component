package eichlerjiri.mapcomponent.shaders;

import static android.opengl.GLES20.*;
import static eichlerjiri.mapcomponent.utils.Common.*;

public class MapShader {

    public final int program;

    public final int vertexLoc;
    public final int pvmLoc;
    public final int scaleShiftLoc;

    public MapShader() {
        String vertexShaderSource = "" +
                "attribute vec2 vertex;\n" +
                "uniform mat4 pvm;\n" +
                "uniform vec4 scaleShift;\n" +
                "varying vec2 texCoord;\n" +
                "\n" +
                "void main() {\n" +
                "    texCoord = vec2(vertex.x * scaleShift.x + scaleShift.z," +
                " vertex.y * scaleShift.y + scaleShift.w);\n" +
                "    gl_Position = pvm * vec4(vertex, 0, 1);\n" +
                "}\n";

        String fragmentShaderSource = "precision mediump float;" +
                "varying vec2 texCoord;\n" +
                "uniform sampler2D texture;\n" +
                "\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D(texture, texCoord);\n" +
                "}\n";

        program = createProgram(vertexShaderSource, fragmentShaderSource);

        vertexLoc = glGetAttribLocation(program, "vertex");
        pvmLoc = glGetUniformLocation(program, "pvm");
        scaleShiftLoc = glGetUniformLocation(program, "scaleShift");
    }
}
