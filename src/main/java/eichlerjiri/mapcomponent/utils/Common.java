package eichlerjiri.mapcomponent.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

import static android.opengl.GLES20.*;
import static java.lang.Math.*;

public class Common {

    public static double mercatorPixelSize(float tileSize, float zoom) {
        return 1 / (tileSize * pow(2, zoom));
    }

    public static float computeDistance(float x1, float y1, float x2, float y2) {
        float xDiff = x1 - x2;
        float yDiff = y1 - y2;
        return (float) sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    public static float spSize(Context context) {
        return context.getResources().getDisplayMetrics().scaledDensity;
    }

    public static LoadedTile decodeTile(int zoom, int x, int y, byte[] data) {
        if (data == null) {
            return new LoadedTile(zoom, x, y, 0, 0, null);
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bitmap == null) {
            return new LoadedTile(zoom, x, y, 0, 0, null);
        }

        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            Log.w("Common", "Converting bitmap");
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(buffer);
        buffer.rewind();

        return new LoadedTile(zoom, x, y, bitmap.getWidth(), bitmap.getHeight(), buffer);
    }

    public static int createProgram(String vertexShaderSource, String fragmentShaderSource) {
        int vertexShaderId = prepareShader(GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShaderId = prepareShader(GL_FRAGMENT_SHADER, fragmentShaderSource);

        int programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);
        return programId;
    }

    public static int prepareShader(int type, String shaderCode) {
        int shaderId = glCreateShader(type);

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        return shaderId;
    }

    public static byte[] download(String url) throws InterruptedIOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:64.0) Gecko/20100101 Firefox/64.0");
            InputStream is = conn.getInputStream();
            try {
                return readAll(is);
            } finally {
                closeStream(is);
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot download file: " + url, e);
            if (conn != null) {
                InputStream es = conn.getErrorStream();
                if (es != null) {
                    readAll(es);
                    closeStream(es);
                }
            }
            return null;
        }
    }

    public static byte[] readFile(File file) throws InterruptedIOException {
        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                return readAll(fis);
            } finally {
                closeStream(fis);
            }
        } catch (FileNotFoundException e) {
            Log.e("Common", "Cannot read file: " + file, e);
            return null;
        }
    }

    public static void writeFile(File file, byte[] data) throws InterruptedIOException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(data);
            } finally {
                closeStream(fos);
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot write file: " + file, e);
        }
    }

    public static byte[] readAll(InputStream is) throws InterruptedIOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
            byte[] buffer = new byte[4096];
            int num;
            while ((num = is.read(buffer)) != -1) {
                baos.write(buffer, 0, num);
            }
            return baos.toByteArray();
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot read stream", e);
            return null;
        }
    }

    public static void closeStream(Closeable is) throws InterruptedIOException {
        try {
            is.close();
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot close stream", e);
        }
    }

    public static void multiplyMM(float[] result, float[] first, float[] second) {
        for (int i = 0; i < 16; i += 4) {
            for (int j = 0; j < 4; j++) {
                result[i + j] = first[j] * second[i]
                        + first[j + 4] * second[i + 1]
                        + first[j + 8] * second[i + 2]
                        + first[j + 12] * second[i + 3];
            }
        }
    }
}
