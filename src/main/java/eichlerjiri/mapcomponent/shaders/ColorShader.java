package eichlerjiri.mapcomponent.shaders;

import static android.opengl.GLES20.*;
import static eichlerjiri.mapcomponent.utils.Common.*;

public class ColorShader {

    private final int programId;

    private final int vertexLoc;
    private final int pvmLoc;
    private final int colorLoc;

    public ColorShader() {
        String vertexShaderSource = "" +
                "attribute vec2 vertex;\n" +
                "uniform mat4 pvm;\n" +
                "\n" +
                "void main() {\n" +
                "    gl_Position = pvm * vec4(vertex,0,1);\n" +
                "}\n";

        String fragmentShaderSource = "precision mediump float;\n" +
                "uniform vec4 color;\n" +
                "\n" +
                "void main() {\n" +
                "    gl_FragColor = color;\n" +
                "}\n";

        programId = createProgram(vertexShaderSource, fragmentShaderSource);

        vertexLoc = glGetAttribLocation(programId, "vertex");
        pvmLoc = glGetUniformLocation(programId, "pvm");
        colorLoc = glGetUniformLocation(programId, "color");
    }

    public void render(float[] pvm, int buffer, int bufferCount, int drawType,
                       float r, float g, float b, float a) {
        glUseProgram(programId);
        glEnableVertexAttribArray(vertexLoc);

        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glVertexAttribPointer(vertexLoc, 2, GL_FLOAT, false, 0, 0);

        glUniform4f(colorLoc, r, g, b, a);

        glUniformMatrix4fv(pvmLoc, 1, false, pvm, 0);

        glDrawArrays(drawType, 0, bufferCount);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDisableVertexAttribArray(vertexLoc);
        glUseProgram(0);
    }
}
