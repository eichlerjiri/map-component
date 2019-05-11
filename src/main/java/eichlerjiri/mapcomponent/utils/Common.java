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
import java.net.URLConnection;
import java.nio.ByteBuffer;

import static android.opengl.GLES20.*;

public class Common {

    public static double mercatorPixelSize(float tileSize, float zoom) {
        return 1 / (tileSize * Math.pow(2, zoom));
    }

    public static float computeDistance(float x1, float y1, float x2, float y2) {
        float xDiff = x1 - x2;
        float yDiff = y1 - y2;
        return (float) Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    public static float spSize(Context context) {
        return context.getResources().getDisplayMetrics().scaledDensity;
    }

    public static LoadedTile decodeTile(TileKey tileKey, byte[] data) {
        if (data == null) {
            return new LoadedTile(tileKey, 0, 0, null);
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bitmap == null) {
            return new LoadedTile(tileKey, 0, 0, null);
        }

        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            Log.w("TileLoadPool", "Converting bitmap");
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(buffer);
        buffer.rewind();

        return new LoadedTile(tileKey, bitmap.getWidth(), bitmap.getHeight(), buffer);
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
        URLConnection conn = null;
        try {
            conn = new URL(url).openConnection();
            InputStream is = conn.getInputStream();
            try {
                return readAll(is);
            } finally {
                closeStream(is);
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("IOUtils", "Cannot download file: " + url, e);
            if (conn instanceof HttpURLConnection) {
                InputStream es = ((HttpURLConnection) conn).getErrorStream();
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
            Log.e("IOUtils", "Cannot read file: " + file, e);
            return null;
        }
    }

    public static boolean writeFile(File file, byte[] data) throws InterruptedIOException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(data);
                return true;
            } finally {
                closeStream(fos);
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("IOUtils", "Cannot write file: " + file, e);
            return false;
        }
    }

    public static byte[] readAll(InputStream is) throws InterruptedIOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
            byte[] buffer = new byte[4096];
            while (true) {
                int num = is.read(buffer);
                if (num == -1) {
                    break;
                }
                baos.write(buffer, 0, num);
            }
            return baos.toByteArray();
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("IOUtils", "Cannot read stream", e);
            return null;
        }
    }

    public static void closeStream(Closeable is) throws InterruptedIOException {
        try {
            is.close();
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("IOUtils", "Cannot close stream", e);
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
