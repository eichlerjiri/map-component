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

    private final int programId;

    private final int vertexLoc;
    private final int pvmLoc;

    public MapShader() {
        programId = GLUtils.createProgram(vertexShaderSource, fragmentShaderSource);

        vertexLoc = glGetAttribLocation(programId, "vertex");
        pvmLoc = glGetUniformLocation(programId, "pvm");
    }

    public void render(float[] pvm, int buffer, int bufferCount, int texture) {
        glUseProgram(programId);
        glEnableVertexAttribArray(vertexLoc);

        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glVertexAttribPointer(vertexLoc, 2, GL_FLOAT, false, 0, 0);

        glUniformMatrix4fv(pvmLoc, 1, false, pvm, 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);

        glDrawArrays(GL_TRIANGLE_FAN, 0, bufferCount);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDisableVertexAttribArray(vertexLoc);
        glUseProgram(0);
    }
}
