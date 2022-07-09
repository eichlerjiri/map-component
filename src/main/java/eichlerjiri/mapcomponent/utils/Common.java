package eichlerjiri.mapcomponent.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import static android.opengl.GLES20.*;
import android.util.Log;
import eichlerjiri.mapcomponent.utils.TileKey.LoadedTile;
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
import java.net.UnknownHostException;
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
        try {
            HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
            conn.setSSLSocketFactory(prepareSSLSocketFactory());
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:97.0) Gecko/20100101 Firefox/97.0");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");

            byte[] response = readHTTPResponse(conn);
            int code = conn.getResponseCode();
            if (code != 200) {
                Log.e("Common", "Cannot download file " + url + ": " + code + ": " + conn.getResponseMessage());
                return null;
            }
            return response;
        } catch (InterruptedIOException e) {
            throw e;
        } catch (UnknownHostException e) {
            Log.e("Common", "Cannot download file " + url + ": unknown hostname", e);
            return null;
        } catch (IOException e) {
            Log.e("Common", "Cannot download file " + url, e);
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

    public static byte[] readAllAndClose(InputStream is) throws IOException {
        try {
            return readAll(is);
        } finally {
            is.close();
        }
    }

    public static byte[] readHTTPResponse(HttpURLConnection conn) throws IOException {
        try {
            return readAllAndClose(conn.getInputStream());
        } catch (IOException e) {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                return readAllAndClose(errorStream);
            } else if (conn.getResponseCode() == -1) {
                throw e;
            } else {
                return new byte[]{};
            }
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
