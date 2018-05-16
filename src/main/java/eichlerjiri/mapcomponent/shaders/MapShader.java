package eichlerjiri.mapcomponent.shaders;

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

    public final int vertexShaderId;
    public final int fragmentShaderId;
    public final int programId;

    public final int vertexLoc;
    public final int pvmLoc;

    public MapShader() {
        vertexShaderId = prepareShader(GL_VERTEX_SHADER, vertexShaderSource);
        fragmentShaderId = prepareShader(GL_FRAGMENT_SHADER, fragmentShaderSource);

        programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);

        vertexLoc = glGetAttribLocation(programId, "vertex");
        pvmLoc = glGetUniformLocation(programId, "pvm");
    }

    private int prepareShader(int type, String shaderCode) {
        int shaderId = glCreateShader(type);

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        return shaderId;
    }
}
