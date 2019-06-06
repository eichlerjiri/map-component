package eichlerjiri.mapcomponent;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import eichlerjiri.mapcomponent.shaders.ColorShader;
import eichlerjiri.mapcomponent.shaders.MapShader;
import eichlerjiri.mapcomponent.utils.FloatList;

import static android.opengl.GLES20.*;

public class Drawing {

    public final float spSize;
    public final int[] itmp = new int[1];

    public MapShader mapShader;
    public int mapVbuffer;
    public int mapVbufferCount;

    public ColorShader colorShader;
    public int currentPositionVbuffer;
    public int currentPositionVbufferCount;
    public int nodirCurrentPositionVbuffer;
    public int nodirCurrentPositionVbufferCount;
    public int positionVbuffer;
    public int positionVbufferCount;
    public int pathVbuffer;
    public FloatBuffer pathVtmp;

    public int boundBuffer;
    public int boundTexture;
    public int boundProgram;
    public boolean attribOk;

    public Drawing(float spSize) {
        this.spSize = spSize;
    }

    public static void surfaceChanged(int width, int height) {
        glViewport(0, 0, width, height);
    }

    public void renderTile(float[] pvm, int texture, float scaleX, float scaleY, float shiftX, float shiftY) {
        if (mapShader == null) {
            mapShader = new MapShader();
        }

        if (mapVbuffer == 0) {
            prepareMapBuffer();
        }

        if (boundProgram != mapShader.program) {
            boundProgram = mapShader.program;
            glUseProgram(mapShader.program);
            glEnableVertexAttribArray(mapShader.vertexLoc);
            attribOk = false;
        }

        if (boundBuffer != mapVbuffer) {
            bindBuffer(mapVbuffer);
        }

        if (!attribOk) {
            glVertexAttribPointer(mapShader.vertexLoc, 2, GL_FLOAT, false, 0, 0);
            attribOk = true;
        }

        if (boundTexture != texture) {
            boundTexture = texture;
            glBindTexture(GL_TEXTURE_2D, texture);
        }

        glUniform4f(mapShader.scaleShiftLoc, scaleX, scaleY, shiftX, shiftY);
        glUniformMatrix4fv(mapShader.pvmLoc, 1, false, pvm, 0);
        glDrawArrays(GL_TRIANGLE_FAN, 0, mapVbufferCount);
    }

    public void renderEmptyTile(float[] pvm) {
        if (mapVbuffer == 0) {
            prepareMapBuffer();
        }

        renderColor(pvm, mapVbuffer, mapVbufferCount, GL_TRIANGLE_FAN, 0.8f, 0.97f, 1.0f, 1.0f);
    }

    public void renderCurrentPosition(float[] pvm) {
        if (currentPositionVbuffer == 0) {
            float[] data = new float[]{-12, 12, 0, -4, 0, -12, 12, 12, 0, -4, 0, -12};
            for (int i = 0; i < data.length; i++) {
                data[i] *= spSize;
            }
            currentPositionVbuffer = prepareStaticBuffer(data);
            currentPositionVbufferCount = data.length / 2;
        }

        renderColor(pvm, currentPositionVbuffer, currentPositionVbufferCount, GL_TRIANGLES, 0, 0, 1, 1);
    }

    public void renderCurrentPositionNoDir(float[] pvm) {
        if (nodirCurrentPositionVbuffer == 0) {
            float[] data = new float[]{
                    -12, 0, -8, 0, 0, 12, 0, 12, 0, 8, -8, 0,
                    12, 0, 8, 0, 0, -12, 0, -12, 0, -8, 8, 0,
                    -12, 0, -8, 0, 0, -12, 0, -12, 0, -8, -8, 0,
                    12, 0, 8, 0, 0, 12, 0, 12, 0, 8, 8, 0
            };
            for (int i = 0; i < data.length; i++) {
                data[i] *= spSize;
            }
            nodirCurrentPositionVbuffer = prepareStaticBuffer(data);
            nodirCurrentPositionVbufferCount = data.length / 2;
        }

        renderColor(pvm, nodirCurrentPositionVbuffer, nodirCurrentPositionVbufferCount, GL_TRIANGLES, 0, 0, 1, 1);
    }

    public void renderPosition(float[] pvm, float r, float g, float b, float a) {
        if (positionVbuffer == 0) {
            float[] data = new float[]{0, 0, 10, -20, -10, -20};
            for (int i = 0; i < data.length; i++) {
                data[i] *= spSize;
            }
            positionVbuffer = prepareStaticBuffer(data);
            positionVbufferCount = data.length / 2;
        }

        renderColor(pvm, positionVbuffer, positionVbufferCount, GL_TRIANGLES, r, g, b, a);
    }

    public void renderPath(float[] pvm, FloatList data) {
        if (pathVbuffer == 0) {
            glGenBuffers(1, itmp, 0);
            pathVbuffer = itmp[0];
        }

        if (pathVtmp == null || pathVtmp.array() != data.data) {
            pathVtmp = FloatBuffer.wrap(data.data);
        }

        bindBuffer(pathVbuffer);
        glBufferData(GL_ARRAY_BUFFER, data.size * 4, pathVtmp, GL_DYNAMIC_DRAW);

        renderColor(pvm, pathVbuffer, data.size / 2, GL_TRIANGLES, 0, 0, 0, 1);
    }

    public void noRenderPath() {
        if (pathVbuffer != 0) {
            itmp[0] = pathVbuffer;
            glDeleteBuffers(1, itmp, 0);
            pathVbuffer = 0;
            pathVtmp = null;
        }
    }

    public void renderColor(float[] pvm, int buffer, int bufferCount, int type, float r, float g, float b, float a) {
        if (colorShader == null) {
            colorShader = new ColorShader();
        }

        if (boundProgram != colorShader.program) {
            boundProgram = colorShader.program;
            glUseProgram(colorShader.program);
            glEnableVertexAttribArray(colorShader.vertexLoc);
            attribOk = false;
        }

        if (boundBuffer != buffer) {
            bindBuffer(buffer);
        }

        if (!attribOk) {
            glVertexAttribPointer(colorShader.vertexLoc, 2, GL_FLOAT, false, 0, 0);
            attribOk = true;
        }

        glUniform4f(colorShader.colorLoc, r, g, b, a);
        glUniformMatrix4fv(colorShader.pvmLoc, 1, false, pvm, 0);
        glDrawArrays(type, 0, bufferCount);
    }

    public void prepareMapBuffer() {
        float[] mapBufferData = new float[]{0, 0, 0, 1, 1, 1, 1, 0};
        mapVbuffer = prepareStaticBuffer(mapBufferData);
        mapVbufferCount = mapBufferData.length / 2;
    }

    public int prepareStaticBuffer(float[] data) {
        glGenBuffers(1, itmp, 0);
        bindBuffer(itmp[0]);
        glBufferData(GL_ARRAY_BUFFER, data.length * 4, FloatBuffer.wrap(data), GL_STATIC_DRAW);
        return itmp[0];
    }

    public void bindBuffer(int buffer) {
        boundBuffer = buffer;
        attribOk = false;
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
    }

    public int prepareMapTexture(int width, int height, ByteBuffer data) {
        glGenTextures(1, itmp, 0);
        boundTexture = itmp[0];

        glBindTexture(GL_TEXTURE_2D, boundTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        return boundTexture;
    }

    public void removeMapTexture(int textureId) {
        itmp[0] = textureId;
        glDeleteTextures(1, itmp, 0);
    }
}
