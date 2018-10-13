package eichlerjiri.mapcomponent.shaders;

import eichlerjiri.mapcomponent.utils.GLProxy;

import static android.opengl.GLES20.*;
import static eichlerjiri.mapcomponent.utils.Common.*;

public class ColorShader {

    private final int programId;

    private final int vertexLoc;
    private final int pvmLoc;
    private final int colorLoc;

    public ColorShader(GLProxy gl) {
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

        programId = createProgram(gl, vertexShaderSource, fragmentShaderSource);

        vertexLoc = gl.glGetAttribLocation(programId, "vertex");
        pvmLoc = gl.glGetUniformLocation(programId, "pvm");
        colorLoc = gl.glGetUniformLocation(programId, "color");
    }

    public void render(GLProxy gl, float[] pvm, int buffer, int bufferCount, int drawType,
                       float r, float g, float b, float a) {
        gl.glUseProgram(programId);

        if (gl.attrib1 != buffer) {
            gl.attrib1 = buffer;
            gl.glBindBuffer(GL_ARRAY_BUFFER, buffer);
            gl.glEnableVertexAttribArray(vertexLoc);
            gl.glVertexAttribPointer(vertexLoc, 2, GL_FLOAT, false, 0, 0);
        }

        if (!gl.uni1 || !uniformTest4f(gl.uni1data, r, g, b, a)) {
            gl.uni1 = true;
            gl.glUniform4f(colorLoc, r, g, b, a);
        }

        gl.glUniformMatrix4fv(pvmLoc, 1, false, pvm, 0);

        gl.glDrawArrays(drawType, 0, bufferCount);
    }
}
