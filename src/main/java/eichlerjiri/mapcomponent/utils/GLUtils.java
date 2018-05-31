package eichlerjiri.mapcomponent.utils;

import java.nio.FloatBuffer;

import static android.opengl.GLES20.*;

public class GLUtils {

    public static int createProgram(String vertexShaderSource, String fragmentShaderSource) {
        int vertexShaderId = prepareShader(GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShaderId = prepareShader(GL_FRAGMENT_SHADER, fragmentShaderSource);

        int programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);
        return programId;
    }

    private static int prepareShader(int type, String shaderCode) {
        int shaderId = glCreateShader(type);

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        return shaderId;
    }

    public static int prepareStaticBuffer(float[] data, int[] itmp1) {
        glGenBuffers(1, itmp1, 0);
        int id = itmp1[0];

        glBindBuffer(GL_ARRAY_BUFFER, id);
        glBufferData(GL_ARRAY_BUFFER, data.length * 4, FloatBuffer.wrap(data), GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        return id;
    }
}
