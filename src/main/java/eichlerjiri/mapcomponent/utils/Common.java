package eichlerjiri.mapcomponent.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import static android.opengl.GLES20.*;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import static java.lang.Math.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Common {

    public static float computeDistance(float x1, float y1, float x2, float y2) {
        float xDiff = x1 - x2;
        float yDiff = y1 - y2;
        return (float) sqrt(xDiff * xDiff + yDiff * yDiff);
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
        HttpsURLConnection conn = null;
        try {
            conn = (HttpsURLConnection) new URL(url).openConnection();
            conn.setSSLSocketFactory(prepareSSLSocketFactory());

            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:64.0) Gecko/20100101 Firefox/64.0");
            try (InputStream is = conn.getInputStream()) {
                return readAll(is);
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot download file: " + url, e);
            if (conn != null) {
                readErrorStream(conn);
            }
            return null;
        }
    }

    public static SSLSocketFactory prepareSSLSocketFactory() {
        return new SSLSocketFactory() {
            @Override
            public String[] getDefaultCipherSuites() {
                return ((SSLSocketFactory) SSLSocketFactory.getDefault()).getDefaultCipherSuites();
            }

            @Override
            public String[] getSupportedCipherSuites() {
                return ((SSLSocketFactory) SSLSocketFactory.getDefault()).getSupportedCipherSuites();
            }

            @Override
            public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
                return enableTLS(((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(s, host, port, autoClose));
            }

            @Override
            public Socket createSocket(String host, int port) throws IOException {
                return enableTLS(SSLSocketFactory.getDefault().createSocket(host, port));
            }

            @Override
            public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
                return enableTLS(SSLSocketFactory.getDefault().createSocket(host, port, localHost, localPort));
            }

            @Override
            public Socket createSocket(java.net.InetAddress host, int port) throws IOException {
                return enableTLS(SSLSocketFactory.getDefault().createSocket(host, port));
            }

            @Override
            public Socket createSocket(java.net.InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
                return enableTLS(SSLSocketFactory.getDefault().createSocket(address, port, localAddress, localPort));
            }

            public Socket enableTLS(Socket s) {
                if (s instanceof SSLSocket) {
                    ((SSLSocket) s).setEnabledProtocols(new String[]{"TLSv1.2"});
                }
                return s;
            }
        };
    }

    public static void readErrorStream(HttpURLConnection conn) throws InterruptedIOException {
        try (InputStream es = conn.getErrorStream()) {
            if (es != null) {
                readAll(es);
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot read error response", e);
        }
    }

    public static byte[] readFile(File file) throws InterruptedIOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return readAll(fis);
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot read file: " + file, e);
            return null;
        }
    }

    public static void writeFile(File file, byte[] data) throws InterruptedIOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            Log.e("Common", "Cannot write file: " + file, e);
        }
    }

    public static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
        byte[] buffer = new byte[4096];
        int num;
        while ((num = is.read(buffer)) != -1) {
            baos.write(buffer, 0, num);
        }
        return baos.toByteArray();
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
