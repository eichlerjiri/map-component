package eichlerjiri.mapcomponent.utils;

import android.opengl.GLES20;

import java.nio.Buffer;

import static android.opengl.GLES20.*;

public class GLProxy {

    public int boundArrayBuffer;
    public int bound2DTexture;
    public int activeProgram;
    public int attrib1;
    public boolean uni1;
    public float[] uni1data = new float[4];

    public void glViewport(int x, int y, int width, int height) {
        GLES20.glViewport(x, y, width, height);
    }

    public void glGenBuffers(int n, int[] buffers, int offset) {
        GLES20.glGenBuffers(n, buffers, offset);
    }

    public void glDeleteBuffers(int n, int[] buffers, int offset) {
        for (int i = 0; i < offset; i++) {
            if (buffers[offset + i] == boundArrayBuffer) {
                boundArrayBuffer = 0;
            }
        }
        GLES20.glDeleteBuffers(n, buffers, offset);
    }

    public void glBindBuffer(int target, int buffer) {
        if (target == GL_ARRAY_BUFFER) {
            if (boundArrayBuffer == buffer) {
                return;
            }
            boundArrayBuffer = buffer;
        }
        GLES20.glBindBuffer(target, buffer);
    }

    public void glBufferData(int target, int size, Buffer data, int usage) {
        GLES20.glBufferData(target, size, data, usage);
    }

    public void glGenTextures(int n, int[] textures, int offset) {
        GLES20.glGenTextures(n, textures, offset);
    }

    public void glBindTexture(int target, int texture) {
        if (target == GL_TEXTURE_2D) {
            if (bound2DTexture == texture) {
                return;
            }
            bound2DTexture = texture;
        }
        GLES20.glBindTexture(target, texture);
    }

    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format,
                             int type, Buffer pixels) {
        GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    public void glTexParameteri(int target, int pname, int param) {
        GLES20.glTexParameteri(target, pname, param);
    }

    public void glDeleteTextures(int n, int[] textures, int offset) {
        for (int i = 0; i < n; i++) {
            if (textures[offset + i] == bound2DTexture) {
                bound2DTexture = 0;
            }
        }
        GLES20.glDeleteTextures(n, textures, offset);
    }

    public int glCreateProgram() {
        return GLES20.glCreateProgram();
    }

    public void glAttachShader(int program, int shader) {
        GLES20.glAttachShader(program, shader);
    }

    public void glLinkProgram(int program) {
        GLES20.glLinkProgram(program);
    }

    public int glCreateShader(int type) {
        return GLES20.glCreateShader(type);
    }

    public void glShaderSource(int shader, String string) {
        GLES20.glShaderSource(shader, string);
    }

    public void glCompileShader(int shader) {
        GLES20.glCompileShader(shader);
    }

    public void glUseProgram(int program) {
        if (activeProgram == program) {
            return;
        }
        activeProgram = program;
        attrib1 = 0;
        uni1 = false;
        GLES20.glUseProgram(program);
    }

    public void glEnableVertexAttribArray(int index) {
        GLES20.glEnableVertexAttribArray(index);
    }

    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int offset) {
        GLES20.glVertexAttribPointer(indx, size, type, normalized, stride, offset);
    }

    public void glUniform4f(int location, float x, float y, float z, float w) {
        GLES20.glUniform4f(location, x, y, z, w);
    }

    public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] value, int offset) {
        GLES20.glUniformMatrix4fv(location, count, transpose, value, offset);
    }

    public void glDrawArrays(int mode, int first, int count) {
        GLES20.glDrawArrays(mode, first, count);
    }

    public int glGetAttribLocation(int program, String name) {
        return GLES20.glGetAttribLocation(program, name);
    }

    public int glGetUniformLocation(int program, String name) {
        return GLES20.glGetUniformLocation(program, name);
    }
}
