package eichlerjiri.mapcomponent.shaders;

import eichlerjiri.mapcomponent.utils.GLProxy;

import static android.opengl.GLES20.*;
import static eichlerjiri.mapcomponent.utils.Common.*;

public class MapShader {

    private final int programId;

    private final int vertexLoc;
    private final int pvmLoc;
    private final int scaleShiftLoc;

    public MapShader(GLProxy gl) {
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

        programId = createProgram(gl, vertexShaderSource, fragmentShaderSource);

        vertexLoc = gl.glGetAttribLocation(programId, "vertex");
        pvmLoc = gl.glGetUniformLocation(programId, "pvm");
        scaleShiftLoc = gl.glGetUniformLocation(programId, "scaleShift");
    }

    public void render(GLProxy gl, float[] pvm, int buffer, int bufferCount, int texture, int drawType,
                       float scaleX, float scaleY, float shiftX, float shiftY) {
        gl.glUseProgram(programId);

        if (gl.attrib1 != buffer) {
            gl.attrib1 = buffer;
            gl.glBindBuffer(GL_ARRAY_BUFFER, buffer);
            gl.glEnableVertexAttribArray(vertexLoc);
            gl.glVertexAttribPointer(vertexLoc, 2, GL_FLOAT, false, 0, 0);
        }

        if (!gl.uni1 || !uniformTest4f(gl.uni1data, scaleX, scaleY, shiftX, shiftY)) {
            gl.uni1 = true;
            gl.glUniform4f(scaleShiftLoc, scaleX, scaleY, shiftX, shiftY);
        }

        gl.glUniformMatrix4fv(pvmLoc, 1, false, pvm, 0);

        gl.glBindTexture(GL_TEXTURE_2D, texture);

        gl.glDrawArrays(drawType, 0, bufferCount);
    }
}
