package eichlerjiri.mapcomponent.utils;

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
}
